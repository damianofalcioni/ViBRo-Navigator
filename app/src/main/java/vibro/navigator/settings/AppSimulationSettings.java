package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import vibro.navigator.nav.simulation.NavigationSimulationConfig;

public final class AppSimulationSettings {
    private static final String ENABLED = "simulation_enabled";
    private static final String SPEED = "simulation_speed_mps";
    private static final String POSITION = "simulation_initial_position";

    private AppSimulationSettings() { }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(ENABLED, enabled).apply();
    }

    public static String positionText(Context context) {
        return prefs(context).getString(POSITION, "");
    }

    public static NavigationSimulationConfig read(Context context) {
        return new NavigationSimulationConfig(
                prefs(context).getFloat(SPEED, NavigationSimulationConfig.DEFAULT_SPEED_MPS),
                NavigationSimulationConfig.parsePosition(positionText(context)));
    }

    public static void save(Context context, float speedMps, String position) {
        prefs(context).edit().putFloat(SPEED, speedMps).putString(POSITION, position).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
