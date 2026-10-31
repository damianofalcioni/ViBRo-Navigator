package vibro.navigator.android.power;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28, 33})
public class AndroidNavigationBatteryLoggerTest {
    private final Application app = ApplicationProvider.getApplicationContext();

    @Test
    public void callbacksDoNotPollBatteryAtLocationFrequencyAndLoggingOffResetsSampling() {
        AndroidNavigationBatteryLogger logger = new AndroidNavigationBatteryLogger(app);
        assertFalse(logger.shouldSample(1_000, false));
        assertTrue(logger.shouldSample(1_001, true));
        assertFalse(logger.shouldSample(2_001, true));
        assertFalse(logger.shouldSample(61_000, true));
        assertTrue(logger.shouldSample(61_001, true));
        assertFalse(logger.shouldSample(61_002, false));
        assertTrue(logger.shouldSample(61_003, true));
    }

    @Test
    public void deviceReadingsPreserveUnitsCurrentSignAndChargingContext() {
        BatteryManager battery = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 73);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER, 2_100_000);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW, -170_000);
        shadowOf(battery).setIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE, Integer.MIN_VALUE);
        Intent batteryIntent = new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB)
                .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
                .putExtra(BatteryManager.EXTRA_LEVEL, 70)
                .putExtra(BatteryManager.EXTRA_SCALE, 100)
                .putExtra(BatteryManager.EXTRA_VOLTAGE, 3_900)
                .putExtra(BatteryManager.EXTRA_TEMPERATURE, 310);

        Context context = new ContextWrapper(app) {
            @Override
            public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
                return batteryIntent;
            }
        };
        String measurements = new AndroidNavigationBatteryLogger(context).readMeasurements();
        assertTrue(measurements.contains("levelPercent=73"));
        assertTrue(measurements.contains("levelPercentSource=capacity_property"));
        assertTrue(measurements.contains("broadcastLevelPercent=70"));
        assertTrue(measurements.contains("levelDifferencePercent=3"));
        assertTrue(measurements.contains("chargeCounterStatus=charging"));
        assertTrue(measurements.contains("chargeCounterRaw=2100000"));
        assertTrue(measurements.contains("currentNowRaw=-170000"));
        assertTrue(measurements.contains("currentAverageRaw=unknown"));
        assertTrue(measurements.contains("batteryBehavior=UNAVAILABLE"));
        assertTrue(measurements.contains("actualUnits=unvalidated"));
        assertTrue(measurements.contains("voltageMv=3900"));
        assertTrue(measurements.contains("temperatureTenthsC=310"));
        assertTrue(measurements.contains("plugged=2"));
        assertTrue(measurements.contains("status=2"));
    }

    @Test
    public void missingOrInvalidBroadcastPercentageRemainsUnavailable() {
        for (Intent intent : new Intent[] {new Intent(Intent.ACTION_BATTERY_CHANGED),
                new Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL, 10)
                        .putExtra(BatteryManager.EXTRA_SCALE, 0),
                new Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL, 101)
                        .putExtra(BatteryManager.EXTRA_SCALE, 100)}) {
            String measurements = new AndroidNavigationBatteryLogger(withBatteryIntent(intent)).readMeasurements();
            assertTrue(measurements.contains("broadcastLevelPercent=unknown"));
            assertTrue(measurements.contains("levelDifferencePercent=unknown"));
        }
    }

    @Test
    public void fullBatteryStatusIsChargingContextEvenWithoutAPluggedExtra() {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_FULL);
        String measurements = new AndroidNavigationBatteryLogger(withBatteryIntent(intent)).readMeasurements();
        assertTrue(measurements.contains("chargeCounterStatus=charging"));
    }

    private Context withBatteryIntent(Intent intent) {
        return new ContextWrapper(app) {
            @Override
            public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
                return intent;
            }
        };
    }
}
