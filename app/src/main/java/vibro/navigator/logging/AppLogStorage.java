package vibro.navigator.logging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vibro.navigator.android.storage.AndroidAppStorageDirs;

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
        return buildLogFileName(now, 1);
    }

    @NonNull
    static String buildLogFileName(@NonNull Date now, int collisionIndex) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now);
        String collisionSuffix = collisionIndex > 1 ? "-" + collisionIndex : "";
        return LOG_FILE_PREFIX + timestamp + collisionSuffix + LOG_FILE_SUFFIX;
    }

    @NonNull
    static File legacyInternalLogFile(@NonNull Context context) {
        return new File(new File(AndroidAppStorageDirs.internalFilesDir(context), LOG_DIR), "app-behavior.log");
    }

    @NonNull
    private static File resolveLogDir(@NonNull Context context) {
        File externalBase = AndroidAppStorageDirs.preferredExternalFilesDir(context);
        if (externalBase != null) {
            return new File(externalBase, LOG_DIR);
        }
        return new File(AndroidAppStorageDirs.internalFilesDir(context), LOG_DIR);
    }
}
