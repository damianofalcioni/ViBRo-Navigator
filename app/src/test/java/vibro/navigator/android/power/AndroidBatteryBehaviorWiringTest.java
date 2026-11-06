package vibro.navigator.android.power;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.nav.power.NavigationBatteryBehavior;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28, 33})
public class AndroidBatteryBehaviorWiringTest {
    private static final long MINUTE_MS = 60_000L;
    private final Application app = ApplicationProvider.getApplicationContext();
    private final MutableClock clock = new MutableClock();
    private final Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_PLUGGED, 0)
            .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
            .putExtra(BatteryManager.EXTRA_LEVEL, 80)
            .putExtra(BatteryManager.EXTRA_SCALE, 100);

    @Test
    public void diagnosticsAndTripCacheShareTheExactObservedInterpretation() {
        AndroidBatterySnapshotReader reader = new AndroidBatterySnapshotReader(context(), clock, false);
        AndroidNavigationBatteryLogger logger = new AndroidNavigationBatteryLogger(app);
        logger.useSnapshotReader(reader);
        NavigationBatterySnapshot initial = null;
        for (int i = 0; i < 4; i++) {
            setProperties(100 + 10 * i, 80 - i);
            clock.nowMs = i * MINUTE_MS;
            String diagnostics = logger.readMeasurements();
            NavigationBatterySnapshot snapshot = reader.read();
            assertEquals(100 + 10 * i, snapshot.observation.counter);
            assertEquals(clock.nowMs, snapshot.observedAtElapsedMs);
            assertTrue(diagnostics.contains(snapshot.behavior.describe()));
            assertSame(snapshot, reader.read());
            if (i == 0) {
                initial = snapshot;
            }
        }
        assertEquals(NavigationBatteryBehavior.State.INFERRED, reader.read().behavior.state);
        assertEquals(30f, reader.read().estimatedUsedMilliAmpHours(initial), 0f);
    }

    @Test
    public void confirmedPowerContextIsRequiredAndRawChargingReadingsAreRetained() {
        setProperties(100, 80);
        AndroidBatterySnapshotReader reader = new AndroidBatterySnapshotReader(context(), clock, false);
        reader.sampleDiagnostics();
        NavigationBatterySnapshot initial = reader.read();
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING);
        clock.nowMs = MINUTE_MS;
        assertTrue(reader.sampleDiagnostics().contains("chargeCounterRaw=100"));
        assertFalse(reader.read().hasLevelPercent());
        assertFalse(reader.read().sameDischargeEpoch(initial));
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        clock.nowMs += MINUTE_MS;
        assertTrue(reader.sampleDiagnostics().contains("power_state_unknown"));
    }

    @Test
    public void batteryBroadcastsLearnWithoutDiagnosticSamplingAndStopClearsTheCache() {
        setProperties(100, 80);
        shadowOf(app).grantPermissions(app.getPackageName() + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        AndroidBatterySnapshotReader reader = new AndroidBatterySnapshotReader(app, clock);
        for (int i = 0; i < 4; i++) {
            setProperties(100 + 10 * i, 80 - i);
            clock.nowMs = i * MINUTE_MS;
            app.sendBroadcast(intent);
            shadowOf(Looper.getMainLooper()).idle();
        }
        assertEquals(reader.read().behavior.reason, NavigationBatteryBehavior.State.INFERRED, reader.read().behavior.state);
        reader.stop();
        assertNull(reader.read().observation);
    }

    @Test
    public void unsupportedCounterKeepsPercentageAndInvalidPropertyUsesBroadcastFallback() {
        setProperties(Integer.MIN_VALUE, 101);
        AndroidBatterySnapshotReader reader = new AndroidBatterySnapshotReader(context(), clock, false);
        assertTrue(reader.sampleDiagnostics().contains("levelPercentSource=broadcast"));
        assertEquals(80, reader.read().levelPercent);
        assertFalse(reader.read().hasObservedDischarge());
        assertEquals(NavigationBatteryBehavior.State.UNAVAILABLE, reader.read().behavior.state);
    }

    private void setProperties(int counter, int level) {
        BatteryManager battery = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, level);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER, counter);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW, -600);
    }

    private Context context() {
        return new ContextWrapper(app) {
            @Override
            public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) { return intent; }
        };
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs;
        @Override public long elapsedRealtimeMs() { return nowMs; }
    }
}
