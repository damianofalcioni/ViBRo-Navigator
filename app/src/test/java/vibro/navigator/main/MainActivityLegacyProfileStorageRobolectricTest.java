package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;
import android.widget.Spinner;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O, Build.VERSION_CODES.P})
public class MainActivityLegacyProfileStorageRobolectricTest {
    private static final String READ_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String PROFILE_NAME = "legacy-custom";
    private File profileFile;

    @Before
    public void setUp() throws Exception {
        Application context = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(context);
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        PackageInfo brouter = new PackageInfo();
        brouter.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(context.getPackageManager()).installPackage(brouter);
        File directory = new File(AndroidStorageVolumes.storageRoot(context, "primary"),
                "Android/data/btools.routingapp/files/brouter/profiles2");
        assertTrue(directory.mkdirs() || directory.isDirectory());
        profileFile = new File(directory, PROFILE_NAME + ".brf");
        try (FileOutputStream output = new FileOutputStream(profileFile)) {
            output.write("assign valid = true".getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void cleanUp() {
        if (profileFile != null) {
            profileFile.delete();
        }
    }

    @Test
    public void startupUsesAllowPermissionAndRefreshesExternalProfilesOnGrant() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ShadowActivity.PermissionsRequest request = shadowOf(activity).getLastRequestedPermission();
            assertNotNull(request);
            assertEquals(READ_STORAGE, request.requestedPermissions[0]);
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertNoFolderPicker(activity);

            shadowOf((Application) activity.getApplicationContext()).grantPermissions(READ_STORAGE);
            activity.onRequestPermissionsResult(request.requestCode, request.requestedPermissions, new int[]{0});

            assertProfileListed(activity);
        }
    }

    @Test
    public void permissionAlreadyGrantedForStreetsSkipsSetup() {
        shadowOf((Application) ApplicationProvider.getApplicationContext()).grantPermissions(READ_STORAGE);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNull(shadowOf(controller.get()).getLastRequestedPermission());
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertProfileListed(controller.get());
        }
    }

    @Test
    public void returningFromSettingsRefreshesProfilesAfterGrant() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.pause();
            shadowOf((Application) controller.get().getApplicationContext()).grantPermissions(READ_STORAGE);
            controller.resume();
            assertProfileListed(controller.get());
        }
    }

    @Test
    public void cancellingPermissionDoesNotOpenFolderPicker() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ShadowActivity.PermissionsRequest request = shadowOf(activity).getLastRequestedPermission();
            activity.onRequestPermissionsResult(request.requestCode, request.requestedPermissions, new int[0]);
            shadowOf(Looper.getMainLooper()).idle();

            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertNoFolderPicker(activity);
            assertEquals(activity.getString(R.string.msg_brouter_profiles_storage_permission_required),
                    ShadowToast.getTextOfLatestToast());
            assertTrue(((Spinner) activity.findViewById(R.id.profileSpinner)).getCount() > 0);
        }
    }

    private static void assertProfileListed(MainActivity activity) {
        Spinner profiles = activity.findViewById(R.id.profileSpinner);
        for (int index = 0; index < profiles.getCount(); index++) {
            if (PROFILE_NAME.equals(profiles.getItemAtPosition(index).toString())) {
                return;
            }
        }
        throw new AssertionError("Granted legacy profile missing from selector");
    }

    private static void assertNoFolderPicker(MainActivity activity) {
        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertTrue(started == null || !Intent.ACTION_OPEN_DOCUMENT_TREE.equals(started.intent.getAction()));
    }
}
