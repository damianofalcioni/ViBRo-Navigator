package vibro.navigator.android.power;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import vibro.navigator.nav.power.NavigationBatteryObservation;

/** One capture feeds both inference and diagnostics, preserving vendor values verbatim. */
final class AndroidBatteryReadings {
    private static final int UNKNOWN = Integer.MIN_VALUE;
    final NavigationBatteryObservation observation;
    final String details;

    private AndroidBatteryReadings(NavigationBatteryObservation observation, String details) {
        this.observation = observation;
        this.details = details;
    }

    static AndroidBatteryReadings read(Context context, Intent intent, long nowMs) {
        BatteryManager battery = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int propertyLevel = property(battery, BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int broadcastLevel = broadcastLevel(intent);
        NavigationBatteryObservation observation = new NavigationBatteryObservation(
                property(battery, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                property(battery, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                isPercent(propertyLevel) ? propertyLevel : broadcastLevel, nowMs, powerState(intent));
        String comparison = isPercent(propertyLevel) && isPercent(broadcastLevel)
                ? Integer.toString(propertyLevel - broadcastLevel) : "unknown";
        String details = "powerReadings=unvalidated rawUnitContract=uAh_uA actualUnits=unvalidated"
                + " levelPercent=" + available(observation.levelPercent)
                + " levelPercentSource=" + (isPercent(propertyLevel) ? "capacity_property" : "broadcast")
                + " broadcastLevelPercent=" + available(broadcastLevel) + " levelDifferencePercent=" + comparison
                + " chargeCounterRaw=" + available(observation.counter)
                + " currentNowRaw=" + available(observation.current)
                + " currentAverageRaw=" + available(property(battery, BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
                + " voltageMv=" + extra(intent, BatteryManager.EXTRA_VOLTAGE)
                + " temperatureTenthsC=" + extra(intent, BatteryManager.EXTRA_TEMPERATURE)
                + " plugged=" + extra(intent, BatteryManager.EXTRA_PLUGGED)
                + " status=" + extra(intent, BatteryManager.EXTRA_STATUS)
                + " batteryObservationElapsedMs=" + nowMs;
        return new AndroidBatteryReadings(observation, details);
    }

    private static NavigationBatteryObservation.PowerState powerState(Intent intent) {
        if (intent == null) {
            return NavigationBatteryObservation.PowerState.UNKNOWN;
        }
        if (AndroidBatterySnapshotReader.isCharging(intent)) {
            return NavigationBatteryObservation.PowerState.CHARGING;
        }
        return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0
                && intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_DISCHARGING
                ? NavigationBatteryObservation.PowerState.DISCHARGING : NavigationBatteryObservation.PowerState.UNKNOWN;
    }

    private static int property(BatteryManager battery, int property) {
        try {
            return battery == null ? UNKNOWN : battery.getIntProperty(property);
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    private static int broadcastLevel(Intent intent) {
        if (intent == null) {
            return UNKNOWN;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level >= 0 && scale > 0 && level <= scale ? Math.round(level * 100f / scale) : UNKNOWN;
    }

    private static boolean isPercent(int level) { return level >= 0 && level <= 100; }

    private static String available(int value) { return value == UNKNOWN ? "unknown" : Integer.toString(value); }

    private static String extra(Intent intent, String name) {
        return available(intent == null ? UNKNOWN : intent.getIntExtra(name, UNKNOWN));
    }
}
