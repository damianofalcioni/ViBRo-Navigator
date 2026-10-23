package vibro.navigator.android.sensor;

import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;

import java.util.ArrayDeque;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.SensorBuilder;
import org.robolectric.shadows.ShadowSensorManager;

import vibro.navigator.dispatch.TaskScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, shadows = AndroidNavigationMotionTriggerTest.MotionSensorManager.class)
public class AndroidNavigationMotionTriggerTest {
    @Test
    public void onlyARegisteredWakeUpSensorCanEnableSleep() {
        Fixture fixture = new Fixture();
        fixture.manager.sensor = null;
        assertFalse(fixture.adapter().arm(() -> fixture.wakes++));
        fixture.manager.sensor = sensor(false);
        assertFalse(fixture.adapter().arm(() -> fixture.wakes++));
        fixture.manager.sensor = sensor(true);
        fixture.manager.registrationSucceeds = false;
        assertFalse(fixture.adapter().arm(() -> fixture.wakes++));
        assertEquals(null, fixture.manager.listener);
    }

    @Test
    public void triggerDispatchesOnNavigationSchedulerOnceAndCanBeRearmed() {
        Fixture fixture = new Fixture();
        AndroidNavigationMotionTrigger adapter = fixture.adapter();
        assertTrue(adapter.arm(() -> fixture.wakes++));
        TriggerEventListener first = fixture.manager.listener;
        first.onTrigger(null);
        first.onTrigger(null);
        assertEquals(0, fixture.wakes);
        fixture.scheduler.drain();
        assertEquals(1, fixture.wakes);
        assertTrue(adapter.arm(() -> fixture.wakes++));
        fixture.manager.listener.onTrigger(null);
        fixture.scheduler.drain();
        assertEquals(2, fixture.wakes);
    }

    @Test
    public void queuedTriggerAfterCancelOrNewRegistrationCannotRestartNavigation() {
        Fixture fixture = new Fixture();
        AndroidNavigationMotionTrigger adapter = fixture.adapter();
        assertTrue(adapter.arm(() -> fixture.wakes++));
        TriggerEventListener retired = fixture.manager.listener;
        retired.onTrigger(null);
        adapter.cancel();
        fixture.scheduler.drain();
        assertEquals(0, fixture.wakes);
        assertTrue(adapter.arm(() -> fixture.wakes++));
        retired.onTrigger(null);
        fixture.scheduler.drain();
        assertEquals(0, fixture.wakes);
        fixture.manager.listener.onTrigger(null);
        fixture.scheduler.drain();
        assertEquals(1, fixture.wakes);
    }

    @Test
    public void platformRegistrationFailureFallsBackWithoutLeavingATrigger() {
        Fixture fixture = new Fixture();
        fixture.manager.registrationThrows = true;
        assertFalse(fixture.adapter().arm(() -> fixture.wakes++));
        assertEquals(1, fixture.manager.registrations);
        assertEquals(null, fixture.manager.listener);
    }

    @Test
    public void sensorLookupFailureDisablesSleepWithoutCrashingNavigation() {
        Fixture fixture = new Fixture();
        fixture.manager.lookupThrows = true;
        assertFalse(fixture.adapter().arm(() -> fixture.wakes++));
        assertEquals(0, fixture.manager.registrations);
        assertEquals(null, fixture.manager.listener);
    }

    private static Sensor sensor(boolean wakeUp) {
        return SensorBuilder.newBuilder().setType(Sensor.TYPE_SIGNIFICANT_MOTION).setWakeUpFlag(wakeUp).build();
    }

    private static final class Fixture {
        // Other tests can cache a manager created under the default shadow. Use a fresh platform
        // instance so these registration tests are independent of suite ordering.
        private final SensorManager platform = (SensorManager) Shadow.newInstanceOf("android.hardware.SystemSensorManager");
        private final MotionSensorManager manager = Shadow.extract(platform);
        private final Context context = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Object getSystemService(String name) {
                return Context.SENSOR_SERVICE.equals(name) ? platform : super.getSystemService(name);
            }
        };
        private final Scheduler scheduler = new Scheduler();
        private int wakes;

        Fixture() {
            manager.sensor = sensor(true);
        }

        AndroidNavigationMotionTrigger adapter() {
            return new AndroidNavigationMotionTrigger(context, scheduler);
        }
    }

    @Implements(className = "android.hardware.SystemSensorManager", isInAndroidSdk = false)
    public static class MotionSensorManager extends ShadowSensorManager {
        private Sensor sensor;
        private TriggerEventListener listener;
        private boolean registrationSucceeds = true;
        private boolean registrationThrows;
        private int registrations;
        private boolean lookupThrows;

        @Implementation
        protected Sensor getDefaultSensor(int type) {
            if (lookupThrows) {
                throw new IllegalStateException("Test sensor lookup failure");
            }
            return type == Sensor.TYPE_SIGNIFICANT_MOTION ? sensor : null;
        }

        @Implementation
        protected boolean requestTriggerSensorImpl(TriggerEventListener callback, Sensor requested) {
            registrations++;
            if (registrationThrows) {
                throw new IllegalStateException("Test sensor registration failure");
            }
            listener = callback;
            return requested != null && registrationSucceeds;
        }

        @Implementation
        protected boolean cancelTriggerSensorImpl(TriggerEventListener callback, Sensor requested, boolean disable) {
            listener = null;
            return requested != null;
        }
    }

    private static final class Scheduler implements TaskScheduler {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
        @Override public void post(Runnable runnable) { pending.addLast(runnable); }
        void drain() {
            while (!pending.isEmpty()) {
                pending.removeFirst().run();
            }
        }
    }
}
