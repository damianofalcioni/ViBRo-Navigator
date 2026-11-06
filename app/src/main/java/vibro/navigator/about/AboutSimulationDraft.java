package vibro.navigator.about;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

import vibro.navigator.map.MapPickerActivity;
import vibro.navigator.nav.simulation.NavigationSimulationInput;
import vibro.navigator.poi.Poi;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppSimulationSettings;

final class AboutSimulationDraft {
    private static final String STATE = "simulation_draft";
    String speed = "";
    String position = "";
    boolean imperial;
    boolean open;
    boolean picking;

    void load(Context context) {
        imperial = AppSettings.isImperialUnitsEnabled(context);
        speed = NavigationSimulationInput.formatSpeed(AppSimulationSettings.read(context).speedMps,
                imperial, Locale.getDefault());
        position = AppSimulationSettings.positionText(context);
        picking = false;
    }

    void applyMapResult(Context context, Intent data) {
        Poi poi = MapPickerActivity.parseResult(context, data);
        if (poi != null) { position = String.format(Locale.US, "%.6f, %.6f", poi.lat, poi.lon); }
    }

    void save(Bundle state) {
        Bundle value = new Bundle();
        value.putString("speed", speed);
        value.putString("position", position);
        value.putBoolean("imperial", imperial);
        value.putBoolean("open", open);
        value.putBoolean("picking", picking);
        state.putBundle(STATE, value);
    }

    void restore(Bundle state) {
        Bundle value = state == null ? null : state.getBundle(STATE);
        if (value == null) { return; }
        speed = value.getString("speed", "");
        position = value.getString("position", "");
        imperial = value.getBoolean("imperial");
        open = value.getBoolean("open");
        picking = value.getBoolean("picking");
    }
}
