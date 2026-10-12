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
    private static final String FIRST_RUN_SETUP_FILE = "first_run_setup_pending";

    private AppMainUiSettings() {
    }

    public static boolean isWelcomeCompleted(@NonNull Context context) {
        return welcomeCompletionFile(context).isFile();
    }

    public static void completeWelcome(@NonNull Context context) {
        createInstallationFile(context, WELCOME_COMPLETION_FILE);
    }

    public static void startFirstRunSetup(@NonNull Context context) {
        createInstallationFile(context, FIRST_RUN_SETUP_FILE);
    }

    public static boolean isFirstRunSetupPending(@NonNull Context context) {
        return new File(AndroidAppStorageDirs.noBackupFilesDir(context), FIRST_RUN_SETUP_FILE).isFile();
    }

    public static void completeFirstRunSetup(@NonNull Context context) {
        File pending = new File(AndroidAppStorageDirs.noBackupFilesDir(context), FIRST_RUN_SETUP_FILE);
        if (pending.exists() && !pending.delete()) {
            AppLogger.w("AppMainUiSettings", "Unable to save first-run setup completion");
        }
    }

    private static void createInstallationFile(@NonNull Context context, @NonNull String name) {
        try {
            new File(AndroidAppStorageDirs.noBackupFilesDir(context), name).createNewFile();
        } catch (IOException error) {
            AppLogger.e("AppMainUiSettings", "Unable to save installation state: " + name, error);
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
