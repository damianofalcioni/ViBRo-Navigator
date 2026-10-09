package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import vibro.navigator.android.storage.AndroidAppStorageDirs;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRoutingMode;

public final class AppMainUiSettings {
    private static final String KEY_MAIN_UI_ROUTING_MODE = "main_ui_routing_mode";
    private static final String WELCOME_COMPLETION_FILE = "welcome_completed";

    private AppMainUiSettings() {
    }

    public static boolean isWelcomeCompleted(@NonNull Context context) {
        return welcomeCompletionFile(context).isFile();
    }

    public static void completeWelcome(@NonNull Context context) {
        try {
            welcomeCompletionFile(context).createNewFile();
        } catch (IOException error) {
            AppLogger.e("AppMainUiSettings", "Unable to save welcome completion", error);
        }
    }

    @NonNull
    private static File welcomeCompletionFile(@NonNull Context context) {
        // Installation state must not be restored from Android or in-app backups.
        return new File(AndroidAppStorageDirs.noBackupFilesDir(context), WELCOME_COMPLETION_FILE);
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
