package com.vibenavigator.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AppLogger {

    private static final String TAG = "AppLogger";
    private static final Object LOCK = new Object();
    private static final long MAX_LOG_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_LOG_BYTES = 3L * 1024L * 1024L;
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "vibe-navigator-log-";
    private static final String LOG_FILE_SUFFIX = ".txt";
    private static final String PREFS = "vibenavigator_developer";
    private static final String KEY_DEVELOPER_MODE = "developer_mode_enabled";
    private static final String KEY_LOG_FILE_NAME = "log_file_name";

    @Nullable
    private static File logFile;
    private static boolean developerModeEnabled;

    private AppLogger() {
    }

    public static void init(@NonNull Context context) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
            developerModeEnabled = readDeveloperModeEnabled(appContext);
            if (!developerModeEnabled) {
                logFile = null;
                return;
            }
            ensureLogFileLocked(appContext, false);
        }
    }

    @NonNull
    public static String getLogFilePath(@NonNull Context context) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
            developerModeEnabled = readDeveloperModeEnabled(appContext);
            if (developerModeEnabled) {
                ensureLogFileLocked(appContext, false);
            }
            if (logFile != null) {
                return logFile.getAbsolutePath();
            }
        }
        return new File(
                resolveLogDir(context.getApplicationContext()),
                buildLogFileName(new Date())
        ).getAbsolutePath();
    }

    public static boolean isDeveloperModeEnabled(@NonNull Context context) {
        synchronized (LOCK) {
            developerModeEnabled = readDeveloperModeEnabled(context.getApplicationContext());
            return developerModeEnabled;
        }
    }

    public static boolean enableDeveloperMode(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            developerModeEnabled = readDeveloperModeEnabled(appContext);
            if (developerModeEnabled) {
                ensureLogFileLocked(appContext, true);
                writeCurrentLogFileMarkerLocked("Developer log restarted");
                return false;
            }
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_DEVELOPER_MODE, true)
                    .putString(KEY_LOG_FILE_NAME, buildLogFileName(new Date()))
                    .apply();
            developerModeEnabled = true;
            ensureLogFileLocked(appContext, true);
        }
        write("INFO", TAG, "Developer mode enabled", null);
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
        File target;
        synchronized (LOCK) {
            if (!developerModeEnabled) {
                return;
            }
            target = logFile;
        }
        if (target == null) {
            return;
        }

        StringBuilder block = new StringBuilder();
        block.append(timestamp())
                .append(" ")
                .append(level)
                .append("/")
                .append(tag)
                .append(" [")
                .append(Thread.currentThread().getName())
                .append("] ")
                .append(sanitize(message))
                .append("\n");

        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            block.append(sw);
        }

        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            trimIfNeededLocked(logFile);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(block.toString());
            } catch (Exception ignored) {
                // Logging must never crash the app.
            }
        }
    }

    private static void writeMultiline(
            @NonNull String level,
            @NonNull String tag,
            @NonNull String message,
            @NonNull String body
    ) {
        File target;
        synchronized (LOCK) {
            if (!developerModeEnabled) {
                return;
            }
            target = logFile;
        }
        if (target == null) {
            return;
        }

        StringBuilder block = new StringBuilder();
        block.append(timestamp())
                .append(" ")
                .append(level)
                .append("/")
                .append(tag)
                .append(" [")
                .append(Thread.currentThread().getName())
                .append("] ")
                .append(sanitize(message))
                .append("\n")
                .append(normalizeMultiline(body));
        if (!body.endsWith("\n") && !body.endsWith("\r")) {
            block.append("\n");
        }

        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            trimIfNeededLocked(logFile);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(block.toString());
            } catch (Exception ignored) {
                // Logging must never crash the app.
            }
        }
    }

    @NonNull
    private static String timestamp() {
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

    private static void trimIfNeededLocked(@NonNull File file) {
        if (!file.exists() || file.length() <= MAX_LOG_BYTES) {
            return;
        }

        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (RandomAccessFile source = new RandomAccessFile(file, "r");
             FileOutputStream out = new FileOutputStream(temp, false)) {
            long start = Math.max(0L, source.length() - KEEP_LOG_BYTES);
            source.seek(start);
            if (start > 0L) {
                source.readLine();
            }
            out.write((timestamp() + " INFO/AppLogger [main] log truncated to keep single-file history\n")
                    .getBytes(StandardCharsets.UTF_8));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = source.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (Exception ignored) {
            if (temp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
            return;
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
        //noinspection ResultOfMethodCallIgnored
        temp.renameTo(file);
    }

    @NonNull
    private static File resolveLogDir(@NonNull Context context) {
        File externalBase = resolvePreferredExternalFilesDir(context);
        if (externalBase != null) {
            return new File(externalBase, LOG_DIR);
        }
        return new File(context.getFilesDir(), LOG_DIR);
    }

    @Nullable
    private static File resolvePreferredExternalFilesDir(@NonNull Context context) {
        File[] dirs = context.getExternalFilesDirs(null);
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir == null) {
                    continue;
                }
                if (android.os.Environment.isExternalStorageRemovable(dir)) {
                    return dir;
                }
            }
            for (File dir : dirs) {
                if (dir != null) {
                    return dir;
                }
            }
        }
        return context.getExternalFilesDir(null);
    }

    private static void migrateLegacyInternalLogIfNeeded(@NonNull Context context, @NonNull File targetFile) {
        if (targetFile.exists()) {
            return;
        }

        File legacyFile = new File(new File(context.getFilesDir(), LOG_DIR), "app-behavior.log");
        if (!legacyFile.exists() || legacyFile.equals(targetFile)) {
            return;
        }

        File parent = targetFile.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs() && !parent.exists())) {
            return;
        }

        if (legacyFile.renameTo(targetFile)) {
            return;
        }

        File temp = new File(parent, targetFile.getName() + ".migrate");
        try (RandomAccessFile source = new RandomAccessFile(legacyFile, "r");
             FileOutputStream out = new FileOutputStream(temp, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = source.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (Exception ignored) {
            if (temp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
            }
            return;
        }

        if (!temp.renameTo(targetFile)) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        legacyFile.delete();
    }

    private static void ensureLogFileLocked(@NonNull Context context, boolean forceRefresh) {
        File dir = resolveLogDir(context);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String fileName = prefs.getString(KEY_LOG_FILE_NAME, null);
        if (forceRefresh || fileName == null || fileName.trim().isEmpty()) {
            fileName = buildLogFileName(new Date());
            prefs.edit().putString(KEY_LOG_FILE_NAME, fileName).apply();
        }
        File target = new File(dir, fileName);
        if (logFile != null && logFile.equals(target)) {
            return;
        }
        logFile = target;
        migrateLegacyInternalLogIfNeeded(context, logFile);
    }

    private static void writeCurrentLogFileMarkerLocked(@NonNull String message) {
        if (logFile == null) {
            return;
        }
        String block = timestamp()
                + " INFO/"
                + TAG
                + " ["
                + Thread.currentThread().getName()
                + "] "
                + sanitize(message)
                + "\n";
        trimIfNeededLocked(logFile);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(block);
        } catch (Exception ignored) {
            // Logging must never crash the app.
        }
    }

    private static boolean readDeveloperModeEnabled(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DEVELOPER_MODE, false);
    }

    @NonNull
    private static String buildLogFileName(@NonNull Date now) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(now);
        return LOG_FILE_PREFIX + timestamp + LOG_FILE_SUFFIX;
    }
}
