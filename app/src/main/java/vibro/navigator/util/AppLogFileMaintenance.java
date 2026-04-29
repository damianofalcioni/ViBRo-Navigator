package vibro.navigator.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

final class AppLogFileMaintenance {

    private static final long MAX_LOG_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_LOG_BYTES = 3L * 1024L * 1024L;

    private AppLogFileMaintenance() {
    }

    static void appendBlock(@NonNull File file, @NonNull CharSequence block) {
        trimIfNeeded(file);
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(block.toString());
        } catch (Exception ignored) {
            // Logging must never crash the app.
        }
    }

    static void recreateLogFile(@NonNull File file) {
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

    static void migrateLegacyInternalLogIfNeeded(@NonNull Context context, @NonNull File targetFile) {
        File legacyFile = AppLogStorage.legacyInternalLogFile(context);
        if (!shouldMigrateLegacyLog(legacyFile, targetFile)) {
            return;
        }

        File parent = ensureParentDirectory(targetFile);
        if (parent == null || legacyFile.renameTo(targetFile)) {
            return;
        }

        File temp = copyLegacyLogToTemp(legacyFile, parent, targetFile.getName());
        if (temp == null) {
            return;
        }

        if (!temp.renameTo(targetFile)) {
            deleteIfExists(temp);
            return;
        }
        deleteIfExists(legacyFile);
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

    private static void trimIfNeeded(@NonNull File file) {
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
            out.write((AppLogger.timestamp() + " INFO/AppLogger [main] log truncated to keep single-file history\n")
                    .getBytes(StandardCharsets.UTF_8));
            copyFile(source, out);
        } catch (Exception ignored) {
            deleteIfExists(temp);
            return;
        }

        deleteIfExists(file);
        //noinspection ResultOfMethodCallIgnored
        temp.renameTo(file);
    }
}
