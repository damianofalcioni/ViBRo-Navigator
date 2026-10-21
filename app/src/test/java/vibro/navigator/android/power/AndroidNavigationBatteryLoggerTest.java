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
        assertTrue(measurements.contains("chargeCounterUah=2100000"));
        assertTrue(measurements.contains("currentNowUa=-170000"));
        assertTrue(measurements.contains("currentAverageUa=unknown"));
        assertTrue(measurements.contains("voltageMv=3900"));
        assertTrue(measurements.contains("temperatureTenthsC=310"));
        assertTrue(measurements.contains("plugged=2"));
        assertTrue(measurements.contains("status=2"));
    }
}
