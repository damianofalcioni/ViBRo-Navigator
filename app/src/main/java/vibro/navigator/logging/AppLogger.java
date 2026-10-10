package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vibro.navigator.android.storage.AndroidOutputFolderCleaner;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

public final class AppLogger {

    static final String TAG = "AppLogger";
    private static final Object LOCK = new Object();
    private static final String PREFS_NAME = "app_logging";
    private static final String KEY_LOG_ENABLED = "log_enabled";

    @Nullable
    private static AppLogDestination destination;
    private static volatile boolean loggingEnabled;

    private AppLogger() {
    }

    public static void init(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            replaceDestination(appContext, readLogEnabled(appContext));
        }
    }

    @NonNull
    public static String getLogFilePath(@NonNull Context context) {
        synchronized (LOCK) {
            if (destination != null) {
                return destination.path();
            }
        }
        return AndroidOutputStorage.current(context, Kind.LOGS).label;
    }

    public static boolean isLoggingEnabled(@NonNull Context context) {
        synchronized (LOCK) {
            return loggingEnabled;
        }
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static boolean setLoggingEnabled(@NonNull Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (loggingEnabled == enabled) {
                return false;
            }
            replaceDestination(appContext, enabled);
        }
        if (enabled) {
            write("INFO", TAG, "Logging enabled", null);
        }
        return loggingEnabled == enabled;
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        write("DEBUG", tag, message, null);
    }

    public static void dMultiline(@NonNull String tag, @NonNull String message, @NonNull String body) {
        writeMultiline("DEBUG", tag, message, body);
    }

    public static void i(@NonNull String tag, @NonNull String message) {
        write("INFO", tag, message, null);
    }

    public static void w(@NonNull String tag, @NonNull String message) {
        write("WARN", tag, message, null);
    }

    public static void w(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        write("WARN", tag, message, throwable);
    }

    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        write("ERROR", tag, message, throwable);
    }

    public static void anomaly(
            @NonNull Context context,
            @NonNull String tag,
            @NonNull String message,
            @Nullable Throwable throwable
    ) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
            if (destination == null) {
                destination = AppLogDestination.start(appContext);
            }
            StringBuilder block = buildLogPrefix("ERROR", tag, message);
            appendThrowable(block, throwable);
            AppLogDestination.append(destination, block);
        }
    }

    private static void write(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message,
            @Nullable Throwable throwable
    ) {
        if (!loggingEnabled) {
            return;
        }
        StringBuilder block = buildLogPrefix(level, tag, message);
        appendThrowable(block, throwable);
        appendBlock(block);
    }

    private static void appendThrowable(@NonNull StringBuilder block, @Nullable Throwable throwable) {
        if (throwable == null) {
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        block.append(sw);
    }

    private static void writeMultiline(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message,
            @NonNull String body
    ) {
        if (!loggingEnabled) {
            return;
        }
        StringBuilder block = buildLogPrefix(level, tag, message)
                .append(normalizeMultiline(body));
        if (!body.endsWith("\n") && !body.endsWith("\r")) {
            block.append("\n");
        }
        appendBlock(block);
    }

    @NonNull
    static StringBuilder buildLogPrefix(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message
    ) {
        return new StringBuilder()
                .append(timestamp())
                .append(" ")
                .append(level)
                .append("/")
                .append(tag)
                .append(" [")
                .append(Thread.currentThread().getName())
                .append("] ")
                .append(sanitize(message))
                .append("\n");
    }

    private static void appendBlock(@NonNull CharSequence block) {
        synchronized (LOCK) {
            if (loggingEnabled) {
                AppLogDestination.append(destination, block);
            }
        }
    }

    public static void refreshOutputFolder(@NonNull Context context) {
        synchronized (LOCK) {
            if (destination != null) {
                replaceDestination(context, loggingEnabled);
            }
        }
    }

    public static void clearOutputFolder(Context context, @Nullable String selected) throws IOException {
        synchronized (LOCK) {
            AppLogDestination.close(destination);
            destination = null;
            try {
                AndroidOutputFolderCleaner.clear(context, Kind.LOGS, selected);
            } finally {
                replaceDestination(context, loggingEnabled);
            }
        }
    }

    private static void replaceDestination(Context context, boolean enabled) {
        AppLogDestination.close(destination);
        Context appContext = context.getApplicationContext();
        destination = AppLogDestination.startIfEnabled(appContext, enabled);
        loggingEnabled = enabled && destination != null;
        writeLogEnabled(appContext, loggingEnabled);
    }

    @NonNull
    static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    @NonNull
    private static String sanitize(@NonNull String message) {
        return message.replace("\r", "\\r").replace("\n", "\\n");
    }

    @NonNull
    private static String normalizeMultiline(@NonNull String message) {
        return message.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static boolean readLogEnabled(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOG_ENABLED, false);
    }

    private static void writeLogEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOG_ENABLED, enabled)
                .apply();
    }
}
