package vibro.navigator.android.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;

import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.StorageVolumeBuilder;

import java.io.File;

public final class TestOutputStorageCard {
    public static final String ID = "ABCD-1234";
    public final File media;
    public final Context context;

    public TestOutputStorageCard(Context base) {
        File root = new File(base.getCacheDir(), ID);
        media = new File(root, "Android/media/" + base.getPackageName());
        ShadowEnvironment.setExternalStorageRemovable(media, true);
        ShadowEnvironment.setExternalStorageState(media, Environment.MEDIA_MOUNTED);
        context = new ContextWrapper(base) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public File[] getExternalMediaDirs() {
                return new File[]{TestOutputMediaDirs.primary(base), media};
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Shadows.shadowOf(base.getSystemService(StorageManager.class)).addStorageVolume(
                    new StorageVolumeBuilder(ID, root, "SD card", Process.myUserHandle(), Environment.MEDIA_MOUNTED)
                            .setIsPrimary(false).setIsRemovable(true).setFsUuid(ID).build());
        }
    }

    public void eject() {
        ShadowEnvironment.setExternalStorageState(media, Environment.MEDIA_UNMOUNTED);
    }

    public void insert() {
        ShadowEnvironment.setExternalStorageState(media, Environment.MEDIA_MOUNTED);
    }
}
