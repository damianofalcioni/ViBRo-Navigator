package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AppLogger {

    private static final String TAG = "AppLogger";
    private static final Object LOCK = new Object();
    private static final String PREFS_NAME = "app_logging";
    private static final String KEY_LOG_ENABLED = "log_enabled";

    @Nullable
    private static File logFile;
    private static boolean loggingEnabled;

    private AppLogger() {
    }

    public static void init(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            loggingEnabled = readLogEnabled(appContext);
            logFile = null;
            if (loggingEnabled) {
                ensureLogFileLocked(appContext, true);
                writeSessionInfoLocked(appContext);
            }
        }
    }

    @NonNull
    public static String getLogFilePath(@NonNull Context context) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
            if (loggingEnabled) {
                ensureLogFileLocked(appContext, false);
            }
            if (logFile != null) {
                return logFile.getAbsolutePath();
            }
        }
        return AppLogFiles.fallbackLogFilePath(context.getApplicationContext());
    }

    public static boolean isLoggingEnabled(@NonNull Context context) {
        synchronized (LOCK) {
            return loggingEnabled;
        }
    }

    public static boolean setLoggingEnabled(@NonNull Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (loggingEnabled == enabled) {
                return false;
            }
            writeLogEnabled(appContext, enabled);
            loggingEnabled = enabled;
            if (loggingEnabled) {
                ensureLogFileLocked(appContext, true);
                writeSessionInfoLocked(appContext);
            } else {
                logFile = null;
            }
        }
        if (enabled) {
            write("INFO", TAG, "Logging enabled", null);
        }
        return true;
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

    private static void write(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message,
            @Nullable Throwable throwable
    ) {
        StringBuilder block = buildLogPrefix(level, tag, message);
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            block.append(sw);
        }
        appendBlock(block);
    }

    private static void writeMultiline(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message,
            @NonNull String body
    ) {
        StringBuilder block = buildLogPrefix(level, tag, message)
                .append(normalizeMultiline(body));
        if (!body.endsWith("\n") && !body.endsWith("\r")) {
            block.append("\n");
        }
        appendBlock(block);
    }

    @NonNull
    private static StringBuilder buildLogPrefix(
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
        File target = currentLogFile();
        if (target == null) {
            return;
        }

        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            AppLogFiles.appendBlock(logFile, block);
        }
    }

    @Nullable
    private static File currentLogFile() {
        synchronized (LOCK) {
            if (!loggingEnabled) {
                return null;
            }
            return logFile;
        }
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

    private static void ensureLogFileLocked(@NonNull Context context, boolean forceRefresh) {
        logFile = AppLogFiles.ensureLogFile(context, logFile, forceRefresh);
    }

    private static void writeSessionInfoLocked(@NonNull Context context) {
        if (logFile != null) {
            AppLogFiles.appendBlock(logFile, buildLogPrefix(
                    "INFO",
                    TAG,
                    AppLogSessionInfo.format(context, logFile)
            ));
        }
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
