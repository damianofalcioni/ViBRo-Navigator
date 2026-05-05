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
        String fileName = (forceRefresh || currentLogFile == null)
                ? AppLogStorage.buildLogFileName(new Date())
                : currentLogFile.getName();
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
}
