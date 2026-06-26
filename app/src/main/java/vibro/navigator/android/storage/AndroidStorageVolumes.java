package vibro.navigator.android.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AndroidStorageVolumes {
    private static final String PRIMARY_ROOT_ID = "primary";
    private static final String APP_EXTERNAL_DATA_MARKER =
            File.separator + "Android" + File.separator + "data" + File.separator;

    private AndroidStorageVolumes() {
    }

    @NonNull
    public static List<String> secondaryStorageRootIds(@NonNull Context context) {
        List<String> rootIds = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return rootIds;
        }
        StorageManager storageManager = context.getSystemService(StorageManager.class);
        if (storageManager == null) {
            return rootIds;
        }
        for (StorageVolume volume : storageManager.getStorageVolumes()) {
            String uuid = volume.getUuid();
            if (uuid == null || uuid.trim().isEmpty()) {
                continue;
            }
            if (!rootIds.contains(uuid)) {
                rootIds.add(uuid);
            }
        }
        return rootIds;
    }

    @Nullable
    public static File storageRoot(@NonNull Context context, @NonNull String rootId) {
        File[] appExternalDirs = context.getExternalFilesDirs(null);
        for (int i = 0; i < appExternalDirs.length; i++) {
            File appExternalDir = appExternalDirs[i];
            File root = storageRootFromAppExternalDir(appExternalDir);
            if (root == null) {
                continue;
            }
            if (isRequestedRoot(root, rootId, i)) {
                return root;
            }
        }
        return PRIMARY_ROOT_ID.equals(rootId)
                ? Environment.getExternalStorageDirectory()
                : new File(File.separator + "storage", rootId);
    }

    private static boolean isRequestedRoot(@NonNull File root, @NonNull String rootId, int index) {
        if (PRIMARY_ROOT_ID.equals(rootId)) {
            return index == 0;
        }
        return rootId.equals(root.getName());
    }

    @Nullable
    private static File storageRootFromAppExternalDir(@Nullable File appExternalDir) {
        if (appExternalDir == null) {
            return null;
        }
        String path = appExternalDir.getAbsolutePath();
        int marker = path.indexOf(APP_EXTERNAL_DATA_MARKER);
        if (marker <= 0) {
            return null;
        }
        return new File(path.substring(0, marker));
    }
}
