package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Date;

import vibro.navigator.android.logging.AndroidLogFolderWriter;
import vibro.navigator.android.storage.AndroidOutputFolderAccess;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Owns one live destination, switching to the default only after a selected-folder failure. */
final class AppLogDestination {
    private final Context context;
    private AndroidLogFolderWriter custom;
    private File local;

    private AppLogDestination(Context context) {
        this.context = context.getApplicationContext();
    }

    static AppLogDestination start(Context context) {
        AppLogDestination destination = new AppLogDestination(context);
        if (!destination.openCustom()) {
            destination.local = AppLogFiles.startSession(context);
        }
        return destination;
    }

    @Nullable
    static AppLogDestination startIfEnabled(Context context, boolean enabled) {
        return enabled ? start(context) : null;
    }

    private boolean openCustom() {
        custom = AndroidLogFolderWriter.open(context, AppLogStorage.buildLogFileName(new Date()));
        if (custom == null) {
            return false;
        }
        if (custom.append(AppLogger.buildLogPrefix("INFO", AppLogger.TAG,
                AppLogSessionInfo.formatDestination(context, custom.path())))) {
            local = null;
            return true;
        }
        custom = null;
        return false;
    }

    void append(CharSequence block) {
        if (custom == null && AndroidOutputFolderAccess.tree(context, Kind.LOGS) != null
                && !AndroidOutputFolderAccess.isFailureMarked(context, Kind.LOGS)) {
            openCustom();
        }
        if (custom != null) {
            if (custom.append(block)) {
                return;
            }
            custom = null;
            local = AppLogFiles.startSession(context);
        }
        if (local != null) {
            AppLogFiles.appendBlock(local, block);
        }
    }

    String path() {
        if (custom != null) {
            return custom.path();
        }
        local = AppLogFiles.ensureLogFile(context, local, false);
        return local == null ? AppLogFiles.fallbackLogFilePath(context) : local.getAbsolutePath();
    }

    void close() {
        if (custom != null) {
            custom.close();
        }
    }

    static void close(AppLogDestination destination) {
        if (destination != null) {
            destination.close();
        }
    }
}
