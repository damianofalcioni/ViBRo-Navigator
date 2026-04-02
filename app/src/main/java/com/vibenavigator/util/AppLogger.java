package com.vibenavigator.util;

import android.content.Context;

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

    private static final Object LOCK = new Object();
    private static final long MAX_LOG_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_LOG_BYTES = 3L * 1024L * 1024L;
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "app-behavior.log";

    @Nullable
    private static File logFile;

    private AppLogger() {
    }

    public static void init(@NonNull Context context) {
        synchronized (LOCK) {
            if (logFile != null) {
                return;
            }

            File dir = resolveLogDir(context.getApplicationContext());
            if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
                return;
            }
            logFile = new File(dir, LOG_FILE);
            migrateLegacyInternalLogIfNeeded(context.getApplicationContext(), logFile);
        }
    }

    @NonNull
    public static String getLogFilePath(@NonNull Context context) {
        init(context);
        synchronized (LOCK) {
            if (logFile != null) {
                return logFile.getAbsolutePath();
            }
        }
        return new File(resolveLogDir(context.getApplicationContext()), LOG_FILE).getAbsolutePath();
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        write("DEBUG", tag, message, null);
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

    @NonNull
    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    @NonNull
    private static String sanitize(@NonNull String message) {
        return message.replace("\r", "\\r").replace("\n", "\\n");
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

        File legacyFile = new File(new File(context.getFilesDir(), LOG_DIR), LOG_FILE);
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
}
