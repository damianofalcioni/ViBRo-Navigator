package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavigationRoutingMode;

public final class AppMainUiSettings {
    private static final String KEY_MAIN_UI_ROUTING_MODE = "main_ui_routing_mode";

    private AppMainUiSettings() {
    }

    @NonNull
    public static NavigationRoutingMode getRoutingMode(@NonNull Context context) {
        return getRoutingMode(prefs(context));
    }

    @NonNull
    static NavigationRoutingMode getRoutingMode(@NonNull SharedPreferences preferences) {
        return NavigationRoutingMode.fromSerializedName(preferences.getString(KEY_MAIN_UI_ROUTING_MODE, null));
    }

    public static void setRoutingMode(
            @NonNull Context context,
            @NonNull NavigationRoutingMode routingMode
    ) {
        setRoutingMode(prefs(context), routingMode);
    }

    static void setRoutingMode(
            @NonNull SharedPreferences preferences,
            @NonNull NavigationRoutingMode routingMode
    ) {
        preferences.edit()
                .putString(KEY_MAIN_UI_ROUTING_MODE, routingMode.serializedName())
                .apply();
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
