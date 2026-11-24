package vibro.navigator.about;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.map.MapPickerActivity;
import vibro.navigator.nav.simulation.NavigationSimulationInput;
import vibro.navigator.poi.Poi;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppSimulationSettings;

final class AboutSimulationDraft {
    private static final String STATE = "simulation_draft";
    private static final String SPEED_LANGUAGE = "speed_language";
    String speed = "";
    String position = "";
    Locale speedLocale = Locale.ENGLISH;
    boolean imperial;
    boolean open;
    boolean picking;

    void load(Context context) {
        imperial = AppSettings.isImperialUnitsEnabled(context);
        speedLocale = Locale.forLanguageTag(AndroidAppLanguage.effectiveLanguageTag(context));
        speed = NavigationSimulationInput.formatSpeed(AppSimulationSettings.read(context).speedMps,
                imperial, speedLocale);
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
        value.putString(SPEED_LANGUAGE, speedLocale.toLanguageTag());
        value.putString("position", position);
        value.putBoolean("imperial", imperial);
        value.putBoolean("open", open);
        value.putBoolean("picking", picking);
        state.putBundle(STATE, value);
    }

    void restore(Context context, Bundle state) {
        Bundle value = state == null ? null : state.getBundle(STATE);
        if (value == null) { return; }
        speed = value.getString("speed", "");
        speedLocale = Locale.forLanguageTag(value.getString(SPEED_LANGUAGE, AndroidAppLanguage.effectiveLanguageTag(context)));
        position = value.getString("position", "");
        imperial = value.getBoolean("imperial");
        open = value.getBoolean("open");
        picking = value.getBoolean("picking");
    }
}
