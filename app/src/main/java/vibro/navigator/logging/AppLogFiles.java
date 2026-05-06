package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Date;

final class AppLogFiles {

    private AppLogFiles() {
    }

    @NonNull
    static String fallbackLogFilePath(@NonNull Context context) {
        return AppLogStorage.fallbackLogFilePath(context);
    }

    @Nullable
    static File ensureLogFile(
            @NonNull Context context,
            @Nullable File currentLogFile,
            boolean forceRefresh
    ) {
        File dir = AppLogStorage.ensureLogDir(context);
        if (dir == null) {
            return currentLogFile;
        }
        String fileName = forceRefresh
                ? newSessionLogFileName(dir)
                : currentSessionLogFileName(currentLogFile);
        File target = new File(dir, fileName);
        if (target.equals(currentLogFile)) {
            if (forceRefresh) {
                AppLogFileMaintenance.recreateLogFile(target);
            }
            return currentLogFile;
        }
        if (forceRefresh) {
            AppLogFileMaintenance.recreateLogFile(target);
        } else {
            AppLogFileMaintenance.migrateLegacyInternalLogIfNeeded(context, target);
        }
        return target;
    }

    static void appendBlock(@NonNull File file, @NonNull CharSequence block) {
        AppLogFileMaintenance.appendBlock(file, block);
    }

    @NonNull
    private static String currentSessionLogFileName(@Nullable File currentLogFile) {
        return currentLogFile == null
                ? AppLogStorage.buildLogFileName(new Date(), 1)
                : currentLogFile.getName();
    }

    @NonNull
    private static String newSessionLogFileName(@NonNull File dir) {
        Date now = new Date();
        int collisionIndex = 1;
        String fileName;
        do {
            fileName = AppLogStorage.buildLogFileName(now, collisionIndex);
            collisionIndex++;
        } while (new File(dir, fileName).exists());
        return fileName;
    }
}
