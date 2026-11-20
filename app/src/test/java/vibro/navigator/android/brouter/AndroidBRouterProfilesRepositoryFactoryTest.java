package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfilesRepository;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterProfilesRepositoryFactoryTest {
    private static final String PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_DATA_PROFILES_DIR_ID =
            "primary:Android/data/btools.routingapp/files/brouter/profiles2";
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
    public void persistedSharedLegacyFolderKeepsNamedProfileAndParameters() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            String id = "primary:brouter/profiles2";
            writeProfileFile(activity, id);
            var repository = AndroidBRouterProfilesRepositoryFactory.create();
            assertFalse(repository.listProfiles(activity).contains(PROFILE_NAME));

            var tree = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", id);
            activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            repository.saveProfilesTreeUri(activity, tree);

            assertTrue(repository.listProfiles(activity).contains(PROFILE_NAME));
            assertEquals(PARAMETER_NAME, repository.getProfileParameters(activity, PROFILE_NAME).get(0).name);
        }
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

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void movedProfileAndItsParametersRemainAvailableWhenTheOldGrantOnlyContainsRemote() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            writeProfileFile(activity, LEGACY_DATA_PROFILES_DIR_ID);
            var repository = AndroidBRouterProfilesRepositoryFactory.create();
            saveGrant(activity, repository);
            assertEquals(List.of(PROFILE_NAME), repository.listProfiles(activity));

            moveProfileToMedia(activity);
            Files.writeString(directDirectory(activity, LEGACY_DATA_PROFILES_DIR_ID).toPath().resolve("remote.brf"), "");

            assertEquals(List.of(PROFILE_NAME), repository.listProfiles(activity));
            assertEquals(PARAMETER_NAME, repository.getProfileParameters(activity, PROFILE_NAME).get(0).name);
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void deletedLegacyFolderDoesNotKeepTheReplacementScanIncomplete() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            writeProfileFile(activity, LEGACY_DATA_PROFILES_DIR_ID);
            var repository = AndroidBRouterProfilesRepositoryFactory.create();
            saveGrant(activity, repository);
            moveProfileToMedia(activity);
            Files.delete(directDirectory(activity, LEGACY_DATA_PROFILES_DIR_ID).toPath());

            var results = new ArrayList<AndroidBRouterProfileLoader.Result>();
            var loader = new AndroidBRouterProfileLoader(activity, repository, results::add);
            try {
                loader.refresh();
                var result = results.get(results.size() - 1);
                assertEquals(List.of(PROFILE_NAME), result.profiles());
                assertTrue(result.complete());
            } finally {
                loader.dispose();
            }
        }
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.O, Build.VERSION_CODES.P})
    public void legacyDataProfilesRequireSharedPermissionAndNeedNoFolderGrant() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String legacyDirectory = LEGACY_DATA_PROFILES_DIR_ID;
        writeProfileFile(activity, legacyDirectory);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        assertFalse(repository.listProfiles(activity).contains(PROFILE_NAME));
        assertTrue(repository.getProfileParameters(activity, PROFILE_NAME).isEmpty());

        shadowOf((Application) activity.getApplicationContext()).grantPermissions("android.permission.READ_EXTERNAL_STORAGE");

        assertTrue(repository.listProfiles(activity).contains(PROFILE_NAME));
        assertEquals(PARAMETER_NAME, repository.getProfileParameters(activity, PROFILE_NAME).get(0).name);
        assertEquals(legacyDirectory, DocumentsContract.getDocumentId(repository.getCustomProfilePickerInitialUri(activity)));
        assertFalse(repository.hasPersistedProfilesTreeAccess(activity));
        assertTrue(AndroidBRouterProfilesStorageAccess.hasAccess(activity, repository));
    }

    private static void saveGrant(Activity activity, BRouterProfilesRepository repository) {
        var tree = AndroidDocumentAccess.buildExternalStorageTreeUri(LEGACY_DATA_PROFILES_DIR_ID);
        activity.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        repository.saveProfilesTreeUri(activity, tree);
    }

    private void moveProfileToMedia(Activity activity) throws IOException {
        var destination = directDirectory(activity, PROFILES_DIR_ID).toPath();
        Files.createDirectories(destination);
        profileFile = Files.move(profileFile.toPath(), destination.resolve(PROFILE_FILE_NAME)).toFile();
    }

    private void writeProfileFile(@NonNull Activity activity) throws IOException {
        writeProfileFile(activity, PROFILES_DIR_ID);
    }

    private void writeProfileFile(@NonNull Activity activity, @NonNull String directoryId) throws IOException {
        File directory = directDirectory(activity, directoryId);
        assertTrue(directory.mkdirs() || directory.isDirectory());
        profileFile = new File(directory, PROFILE_FILE_NAME);
        String profileText = "assign " + PARAMETER_NAME + " = false"
                + " # %" + PARAMETER_NAME + "% | Direct media | boolean";
        try (FileOutputStream output = new FileOutputStream(profileFile)) {
            output.write(profileText.getBytes(StandardCharsets.UTF_8));
        }
    }

    @NonNull
    private File directDirectory(@NonNull Activity activity, @NonNull String directoryId) {
        String relativePath = directoryId.substring(directoryId.indexOf(':') + 1);
        File root = AndroidStorageVolumes.storageRoot(activity, "primary");
        assertNotNull(root);
        return new File(root, relativePath);
    }
}
