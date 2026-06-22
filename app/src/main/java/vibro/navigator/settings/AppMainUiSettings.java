package vibro.navigator.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavigationRoutingMode;

public final class AppMainUiSettings {
    private static final String KEY_MAIN_UI_ROUTING_MODE = "main_ui_routing_mode";

    private AppMainUiSettings() {
    }

    @NonNull
    public static NavigationRoutingMode getRoutingMode(@NonNull Context context) {
        return NavigationRoutingMode.fromSerializedName(
                context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                        .getString(KEY_MAIN_UI_ROUTING_MODE, null)
        );
    }

    public static void setRoutingMode(
            @NonNull Context context,
            @NonNull NavigationRoutingMode routingMode
    ) {
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MAIN_UI_ROUTING_MODE, routingMode.serializedName())
                .apply();
    }
}
