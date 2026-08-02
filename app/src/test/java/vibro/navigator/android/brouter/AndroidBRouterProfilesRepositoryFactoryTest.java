package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterProfilesRepositoryFactoryTest {
    private static final String PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";
    private static final String PROFILE_NAME = "codex-direct-media-profile";
    private static final String PROFILE_FILE_NAME = PROFILE_NAME + ".brf";
    private static final String PARAMETER_NAME = "direct_media";

    private File profileFile;

    @After
    public void deleteProfileFile() {
        if (profileFile != null) {
            profileFile.delete();
        }
    }

    @Test
    public void listProfiles_includesDirectMediaProfiles() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        writeProfileFile(activity);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();

        assertTrue(repository.listProfiles(activity).contains(PROFILE_NAME));
    }

    @Test
    public void getProfileParameters_readsDirectMediaProfileText() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        writeProfileFile(activity);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        List<BRouterProfileParameter> parameters = repository.getProfileParameters(activity, PROFILE_NAME);

        assertEquals(1, parameters.size());
        BRouterProfileParameter parameter = parameters.get(0);
        assertEquals(PARAMETER_NAME, parameter.name);
        assertEquals(BRouterProfileParameter.ValueType.BOOLEAN, parameter.valueType);
    }

    private void writeProfileFile(@NonNull Activity activity) throws IOException {
        File directory = directDirectory(activity);
        assertTrue(directory.mkdirs() || directory.isDirectory());
        profileFile = new File(directory, PROFILE_FILE_NAME);
        String profileText = "assign " + PARAMETER_NAME + " = false"
                + " # %" + PARAMETER_NAME + "% | Direct media | boolean";
        try (FileOutputStream output = new FileOutputStream(profileFile)) {
            output.write(profileText.getBytes(StandardCharsets.UTF_8));
        }
    }

    @NonNull
    private File directDirectory(@NonNull Activity activity) {
        String relativePath = PROFILES_DIR_ID.substring(PROFILES_DIR_ID.indexOf(':') + 1);
        File root = AndroidStorageVolumes.storageRoot(activity, "primary");
        assertNotNull(root);
        return new File(root, relativePath);
    }
}
