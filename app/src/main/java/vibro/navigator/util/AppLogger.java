package vibro.navigator.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
    private static final String LOG_FILE_PREFIX = "vibro-navigator-log-";
    private static final String LOG_FILE_SUFFIX = ".txt";

    @Nullable
    private static File logFile;
    private static boolean developerModeEnabled;

    private AppLogger() {
    }

    public static void init(@NonNull Context context) {
        synchronized (LOCK) {
            developerModeEnabled = false;
            logFile = null;
        }
    }

    @NonNull
    public static String getLogFilePath(@NonNull Context context) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
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
            return developerModeEnabled;
        }
    }

    public static boolean enableDeveloperMode(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (developerModeEnabled) {
                return false;
            }
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
            trimIfNeededLocked(logFile);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(block.toString());
            } catch (Exception ignored) {
                // Logging must never crash the app.
            }
        }
    }

    @Nullable
    private static File currentLogFile() {
        synchronized (LOCK) {
            if (!developerModeEnabled) {
                return null;
            }
            return logFile;
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
            File removableDir = firstRemovableExternalFilesDir(dirs);
            if (removableDir != null) {
                return removableDir;
            }
            File firstDir = firstAvailableExternalFilesDir(dirs);
            if (firstDir != null) {
                return firstDir;
            }
        }
        return context.getExternalFilesDir(null);
    }

    @Nullable
    private static File firstRemovableExternalFilesDir(@NonNull File[] dirs) {
        for (File dir : dirs) {
            if (dir != null && android.os.Environment.isExternalStorageRemovable(dir)) {
                return dir;
            }
        }
        return null;
    }

    @Nullable
    private static File firstAvailableExternalFilesDir(@NonNull File[] dirs) {
        for (File dir : dirs) {
            if (dir != null) {
                return dir;
            }
        }
        return null;
    }

    private static void migrateLegacyInternalLogIfNeeded(@NonNull Context context, @NonNull File targetFile) {
        File legacyFile = new File(new File(context.getFilesDir(), LOG_DIR), "app-behavior.log");
        if (!shouldMigrateLegacyLog(legacyFile, targetFile)) {
            return;
        }

        File parent = ensureParentDirectory(targetFile);
        if (parent == null) {
            return;
        }

        if (legacyFile.renameTo(targetFile)) {
            return;
        }

        File temp = copyLegacyLogToTemp(legacyFile, parent, targetFile.getName());
        if (temp == null) {
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

    private static boolean shouldMigrateLegacyLog(@NonNull File legacyFile, @NonNull File targetFile) {
        return !targetFile.exists() && legacyFile.exists() && !legacyFile.equals(targetFile);
    }

    @Nullable
    private static File copyLegacyLogToTemp(
            @NonNull File legacyFile,
            @NonNull File parent,
            @NonNull String targetFileName
    ) {
        File temp = new File(parent, targetFileName + ".migrate");
        try (RandomAccessFile source = new RandomAccessFile(legacyFile, "r");
             FileOutputStream out = new FileOutputStream(temp, false)) {
            copyFile(source, out);
            return temp;
        } catch (Exception ignored) {
            deleteIfExists(temp);
            return null;
        }
    }

    private static void copyFile(@NonNull RandomAccessFile source, @NonNull FileOutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = source.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    private static void deleteIfExists(@NonNull File file) {
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Nullable
    private static File ensureParentDirectory(@NonNull File file) {
        File parent = file.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs() && !parent.exists())) {
            return null;
        }
        return parent;
    }

    private static void ensureLogFileLocked(@NonNull Context context, boolean forceRefresh) {
        File dir = ensureLogDir(context);
        if (dir == null) {
            return;
        }
        String fileName = (forceRefresh || logFile == null) ? buildLogFileName(new Date()) : logFile.getName();
        File target = new File(dir, fileName);
        if (isCurrentLogFile(target)) {
            refreshCurrentLogFileIfNeeded(forceRefresh);
            return;
        }
        logFile = target;
        if (forceRefresh) {
            recreateLogFileLocked(logFile);
        } else {
            migrateLegacyInternalLogIfNeeded(context, logFile);
        }
    }

    @Nullable
    private static File ensureLogDir(@NonNull Context context) {
        File dir = resolveLogDir(context);
        return !dir.exists() && !dir.mkdirs() && !dir.exists() ? null : dir;
    }

    private static boolean isCurrentLogFile(@NonNull File target) {
        return logFile != null && logFile.equals(target);
    }

    private static void refreshCurrentLogFileIfNeeded(boolean forceRefresh) {
        if (forceRefresh) {
            recreateLogFileLocked(logFile);
        }
    }

    private static void recreateLogFileLocked(@NonNull File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            return;
        }
        try (FileOutputStream ignored = new FileOutputStream(file, false)) {
            // Opening a non-append stream truncates any previous log session and creates the file if needed.
        } catch (IOException ignored) {
            // Logging must never crash the app.
        }
    }

    @NonNull
    private static String buildLogFileName(@NonNull Date now) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(now);
        return LOG_FILE_PREFIX + timestamp + LOG_FILE_SUFFIX;
    }
}
