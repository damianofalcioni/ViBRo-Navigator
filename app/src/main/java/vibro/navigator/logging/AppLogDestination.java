package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.Date;

import vibro.navigator.android.logging.AndroidLogFolderWriter;

/** One live selected output; unavailable storage never creates a fallback session. */
final class AppLogDestination {
    private final Context context;
    private AndroidLogFolderWriter custom;

    private AppLogDestination(Context context) {
        this.context = context.getApplicationContext();
    }

    @Nullable
    static AppLogDestination start(Context context) {
        AppLogDestination destination = new AppLogDestination(context);
        return destination.openCustom() ? destination : null;
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
            return true;
        }
        custom.close();
        custom = null;
        return false;
    }

    boolean append(CharSequence block) {
        return custom != null && custom.append(block);
    }

    static void append(AppLogDestination destination, CharSequence block) {
        if (destination != null && !destination.append(block)) {
            AppLogger.setLoggingEnabled(destination.context, false);
        }
    }

    String path() {
        return custom.path();
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
