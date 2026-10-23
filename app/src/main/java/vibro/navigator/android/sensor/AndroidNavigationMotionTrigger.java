package vibro.navigator.android.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;

import androidx.annotation.Nullable;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationMotionTrigger;

/** Uses the hardware wake-up trigger; never keeps the CPU awake to sample acceleration. */
public final class AndroidNavigationMotionTrigger implements NavigationMotionTrigger {
    private static final String TAG = "NavMotionWake";
    @Nullable private final SensorManager manager;
    @Nullable private final Sensor sensor;
    private final TaskScheduler scheduler;
    @Nullable private TriggerEventListener listener;
    private int generation;
    @Nullable private String lastArmResult;

    public AndroidNavigationMotionTrigger(Context context, TaskScheduler scheduler) {
        manager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        sensor = findSensor(manager);
        this.scheduler = scheduler;
        AppLogger.i(TAG, "Motion wake-up capability " + describeSensor());
    }

    @Nullable
    private static Sensor findSensor(@Nullable SensorManager manager) {
        try {
            return manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        } catch (RuntimeException error) {
            AppLogger.w(TAG, "Motion wake-up sensor lookup failed; keeping acquisition active", error);
            return null;
        }
    }

    private String describeSensor() {
        if (sensor == null) {
            return "available=false reason=significant_motion_sensor_unavailable";
        }
        return "available=" + sensor.isWakeUpSensor() + " name=" + sensor.getName()
                + " vendor=" + sensor.getVendor() + " wakeUp=" + sensor.isWakeUpSensor()
                + " reportedPowerMa=" + sensor.getPower();
    }

    private void recordArmResult(String result) {
        if (!result.equals(lastArmResult)) {
            lastArmResult = result;
            AppLogger.i(TAG, "Motion wake-up registration result=" + result + " " + describeSensor());
        }
    }

    @Override
    public boolean arm(Runnable onMotion) {
        cancel();
        if (manager == null || sensor == null || !sensor.isWakeUpSensor()) {
            recordArmResult("unavailable");
            return false;
        }
        int registration = generation;
        listener = new TriggerEventListener() {
            @Override public void onTrigger(TriggerEvent event) {
                scheduler.post(() -> {
                    if (registration == generation && listener == this) {
                        listener = null;
                        onMotion.run();
                    }
                });
            }
        };
        try {
            if (manager.requestTriggerSensor(listener, sensor)) {
                recordArmResult("armed");
                return true;
            }
            recordArmResult("registration_rejected");
        } catch (RuntimeException error) {
            recordArmResult("registration_exception");
            AppLogger.w(TAG, "Cannot arm motion wake-up; keeping location acquisition active", error);
        }
        cancel();
        return false;
    }

    @Override
    public void cancel() {
        generation++;
        TriggerEventListener previous = listener;
        listener = null;
        if (manager != null && previous != null) {
            try {
                manager.cancelTriggerSensor(previous, sensor);
            } catch (RuntimeException error) {
                AppLogger.w(TAG, "Cannot cancel motion wake-up", error);
            }
        }
    }
}
