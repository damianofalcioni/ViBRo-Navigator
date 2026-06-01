package vibro.navigator.android.storage;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class AndroidStorageVolumes {
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
}
