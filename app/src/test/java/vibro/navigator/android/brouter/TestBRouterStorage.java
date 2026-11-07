package vibro.navigator.android.brouter;

import static org.junit.Assert.assertTrue;

import java.io.File;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.android.storage.AndroidStorageVolumes;

public final class TestBRouterStorage {
    private TestBRouterStorage() {
    }

    public static void createPrimaryFolder(String name) {
        File root = AndroidStorageVolumes.storageRoot(ApplicationProvider.getApplicationContext(), "primary");
        File folder = new File(root, "Android/media/btools.routingapp/brouter/" + name);
        assertTrue(folder.isDirectory() || folder.mkdirs());
    }
}
