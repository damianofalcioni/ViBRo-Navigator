package vibro.navigator.android.storage;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public final class AndroidAppStorageDirs {
    private AndroidAppStorageDirs() {
    }

    @NonNull
    public static File internalFilesDir(@NonNull Context context) {
        return context.getFilesDir();
    }

    @Nullable
    public static File preferredExternalFilesDir(@NonNull Context context) {
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
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
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
