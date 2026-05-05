package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class AppLogStorage {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "vibro-navigator-log-";
    private static final String LOG_FILE_SUFFIX = ".txt";

    private AppLogStorage() {
    }

    @NonNull
    static String fallbackLogFilePath(@NonNull Context context) {
        return new File(resolveLogDir(context), buildLogFileName(new Date())).getAbsolutePath();
    }

    @Nullable
    static File ensureLogDir(@NonNull Context context) {
        File dir = resolveLogDir(context);
        return !dir.exists() && !dir.mkdirs() && !dir.exists() ? null : dir;
    }

    @NonNull
    static String buildLogFileName(@NonNull Date now) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(now);
        return LOG_FILE_PREFIX + timestamp + LOG_FILE_SUFFIX;
    }

    @NonNull
    static File legacyInternalLogFile(@NonNull Context context) {
        return new File(new File(context.getFilesDir(), LOG_DIR), "app-behavior.log");
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
}
