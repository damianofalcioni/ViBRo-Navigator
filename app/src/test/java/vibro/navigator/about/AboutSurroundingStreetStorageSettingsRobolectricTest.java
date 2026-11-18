package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.PermissionsRequest;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26)
public class AboutSurroundingStreetStorageSettingsRobolectricTest {
    private static final String LEGACY_STORAGE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String SEGMENTS = "segments4";
    private static final int PERMISSION_GRANTED = 0;
    private static final int PERMISSION_DENIED = -1;

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppCompassSettings.setSurroundingStreetsEnabled(context, false);
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        ShadowToast.reset();
    }

    @Test
    public void surroundingStreetsSwitchShowsBRouterRequiredToastWhenBRouterIsMissing() {
        AboutActivity activity = activity();
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);

        surroundingStreetsSwitch.performClick();

        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertFalse(surroundingStreetsSwitch.isChecked());
        assertNull(shadowOf(activity).getLastRequestedPermission());
        assertEquals(
                activity.getString(R.string.msg_surrounding_streets_brouter_required),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void surroundingStreetsSwitchPersistsAfterLegacyStorageGrant() {
        installBRouterPackage();
        AboutActivity activity = activity();
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);

        surroundingStreetsSwitch.performClick();

        PermissionsRequest request = shadowOf(activity).getLastRequestedPermission();
        assertEquals(AboutSettingsSwitches.REQUEST_SURROUNDING_STREETS_STORAGE, request.requestCode);
        assertEquals(LEGACY_STORAGE_PERMISSION, request.requestedPermissions[0]);
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));

        sendStorageResult(activity, PERMISSION_GRANTED);

        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertTrue(surroundingStreetsSwitch.isChecked());
    }

    @Test
    public void surroundingStreetsSwitchRevertsWhenLegacyStorageDenied() {
        installBRouterPackage();
        AboutActivity activity = activity();
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);

        surroundingStreetsSwitch.performClick();
        sendStorageResult(activity, PERMISSION_DENIED);

        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertFalse(surroundingStreetsSwitch.isChecked());
        assertEquals(
                activity.getString(R.string.msg_compass_surrounding_streets_storage_permission_required),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void surroundingStreetsSwitchRequestsSegmentsTreeOnAndroid11() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        AboutActivity activity = activity();
        Switch surroundingStreetsSwitch = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);

        surroundingStreetsSwitch.performClick();

        continueFromBRouterPrompt(activity);

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(AboutSettingsSwitches.REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE, started.requestCode);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void enableActionTakesPriorityOverPendingDiagnosticsLookup() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        AboutActivity activity = activity();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(
                AboutDiagnosticRenderScheduler.INITIAL_DIAGNOSTIC_RENDER_DELAY_MS + 50,
                java.util.concurrent.TimeUnit.MILLISECONDS);
        activity.findViewById(R.id.aboutPermissionBRouterStorageRow).performClick();
        enableStreets(activity);
        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void streetSwitchConsumesPickerResultAfterRecreation() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        try (var controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            var activity = controller.get();
            activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch).performClick();
            continueFromBRouterPrompt(activity);
            var picker = shadowOf(activity).getNextStartedActivityForResult();
            controller.recreate();
            grantFolder(controller.get(), picker.requestCode);
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(controller.get()));
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R,
            shadows = vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPromptTest.DelayedDiscovery.class)
    public void switchingOffWhileValidatingDoesNotReenableStreets() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        try (var controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            AboutActivity activity = controller.get();
            Switch setting = activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch);
            setting.performClick();
            var main = shadowOf(android.os.Looper.getMainLooper());
            main.idleFor(800, java.util.concurrent.TimeUnit.MILLISECONDS);
            continueFromBRouterPrompt(activity);
            int request = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            grantFolder(activity, request);
            assertTrue(setting.isChecked());
            setting.performClick();

            main.idleFor(800, java.util.concurrent.TimeUnit.MILLISECONDS);

            assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
            assertFalse(setting.isChecked());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R,
            shadows = vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPromptTest.DelayedDiscovery.class)
    public void turningOffDuringDiscoveryCancelsThePrompt() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        try (var controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            Switch setting = controller.get().findViewById(R.id.aboutCompassSurroundingStreetsSwitch);
            setting.performClick();
            setting.performClick();
            shadowOf(android.os.Looper.getMainLooper()).idleFor(800, java.util.concurrent.TimeUnit.MILLISECONDS);

            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertFalse(setting.isChecked());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void pickerSurvivesAnotherSnapshotBeforeSettingsViewsInflate() {
        installBRouterPackage();
        TestBRouterStorage.createPrimaryFolder(SEGMENTS);
        var initial = new android.os.Bundle();
        var next = new android.os.Bundle();
        int request;
        try (var original = org.robolectric.Robolectric.buildActivity(AboutActivity.class).setup()) {
            shadowOf(android.os.Looper.getMainLooper()).idleFor(
                    AboutActivity.SETTINGS_INITIALIZATION_DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            AboutActivity activity = original.get();
            activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch).performClick();
            continueFromBRouterPrompt(activity);
            request = shadowOf(activity).getNextStartedActivityForResult().requestCode;
            original.pause().stop().saveInstanceState(initial);
        }
        try (var replacement = org.robolectric.Robolectric.buildActivity(AboutActivity.class)
                .create(initial).start().resume()) {
            assertNull(replacement.get().findViewById(R.id.aboutCompassSurroundingStreetsSwitch));
            replacement.pause().stop().saveInstanceState(next);
        }
        try (var restored = org.robolectric.Robolectric.buildActivity(AboutActivity.class).setup(next)) {
            grantFolder(restored.get(), request);
            assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(restored.get()));
        }
    }

    private static void enableStreets(AboutActivity activity) {
        activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch).performClick();
        continueFromBRouterPrompt(activity);
        var picker = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(AboutSettingsSwitches.REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE, picker.requestCode);
        grantFolder(activity, picker.requestCode);
    }

    private static void grantFolder(AboutActivity activity, int requestCode) {
        var tree = vibro.navigator.android.storage.AndroidDocumentAccess
                .buildExternalStorageTreeUri("primary:Android/media/btools.routingapp/brouter");
        activity.onActivityResult(requestCode, android.app.Activity.RESULT_OK,
                new Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    private static AboutActivity activity() {
        return AboutActivityTestSupport.setupWithSettings();
    }

    private static void continueFromBRouterPrompt(AboutActivity activity) {
        shadowOf(android.os.Looper.getMainLooper()).idleFor(android.view.ViewConfiguration.getPressedStateDuration(),
                java.util.concurrent.TimeUnit.MILLISECONDS);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.action_continue),
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()
        );
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains("brouter"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    private static void sendStorageResult(AboutActivity activity, int grantResult) {
        activity.onRequestPermissionsResult(
                AboutSettingsSwitches.REQUEST_SURROUNDING_STREETS_STORAGE,
                new String[]{LEGACY_STORAGE_PERMISSION},
                new int[]{grantResult}
        );
    }

    private static void installBRouterPackage() {
        Application context = ApplicationProvider.getApplicationContext();
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowPackageManager.installPackage(packageInfo);
    }
}
