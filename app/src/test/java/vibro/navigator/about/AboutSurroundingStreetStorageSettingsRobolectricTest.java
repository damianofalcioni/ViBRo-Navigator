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
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
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
    public void diagnosticShowsLegacyStreetStorageStatusWhenSurroundingStreetsNeedIt() {
        installBRouterPackage();
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();
        View row = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow);
        TextView status = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageStatus);

        assertEquals(View.VISIBLE, row.getVisibility());
        assertEquals(activity.getString(R.string.permission_status_needs_attention), status.getText().toString());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void diagnosticShowsSegmentsTreeStatusWhenSurroundingStreetsNeedModernStorageGrant() {
        installBRouterPackage();
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();
        View row = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow);
        TextView status = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageStatus);

        assertEquals(View.VISIBLE, row.getVisibility());
        assertEquals(activity.getString(R.string.permission_status_needs_attention), status.getText().toString());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void diagnosticShowsSegmentsTreeStatusWhenModernStorageGrantIsMissingEvenIfSettingIsOff() {
        installBRouterPackage();
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();
        View row = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow);
        TextView status = activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageStatus);

        assertEquals(View.VISIBLE, row.getVisibility());
        assertEquals(activity.getString(R.string.permission_status_needs_attention), status.getText().toString());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void diagnosticSegmentsTreeRowRequestsFolderAccessOnAndroid11() {
        installBRouterPackage();
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();

        activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow).performClick();

        continueFromBRouterPrompt(activity);

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(AboutPermissionStatusRows.REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE, started.requestCode);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
    }

    @Test
    public void diagnosticHidesLegacyStreetStorageStatusWhenSurroundingStreetsAreOff() {
        installBRouterPackage();
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();

        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow).getVisibility());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void diagnosticHidesStreetStorageStatusWhenBRouterIsMissing() {
        Application context = ApplicationProvider.getApplicationContext();
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();

        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow).getVisibility());
    }

    private static AboutActivity activity() {
        return AboutActivityTestSupport.setupWithSettings();
    }

    private static void continueFromBRouterPrompt(AboutActivity activity) {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.action_continue),
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString()
        );
        assertTrue(String.valueOf(shadowOf(dialog).getMessage()).contains("segments4"));
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

    private static void idleInitialDiagnosticRender() {
        shadowOf(android.os.Looper.getMainLooper()).idleFor(
                AboutDiagnosticRenderScheduler.INITIAL_DIAGNOSTIC_RENDER_DELAY_MS + 50,
                java.util.concurrent.TimeUnit.MILLISECONDS
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
