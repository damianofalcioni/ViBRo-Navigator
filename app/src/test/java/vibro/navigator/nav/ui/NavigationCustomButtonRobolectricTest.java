package vibro.navigator.nav.ui;

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
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.view.View;
import android.widget.ImageButton;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.PermissionsRequest;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNotificationSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationCustomButtonRobolectricTest {
    private static final String LEGACY_STORAGE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE";
    private static final int PERMISSION_GRANTED = 0;

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppNavigationCustomButtonSettings.setEnabled(context, false);
        AppNavigationCustomButtonSettings.setTarget(context, Target.DYNAMIC_GPS_INTERVAL);
        AppCompassSettings.setSurroundingStreetsEnabled(context, false);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, true);
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        ShadowToast.reset();
    }

    @Test
    public void customButtonIsHiddenWhenSettingIsDisabled() {
        TestNavigationActivity activity = activity();
        View customButton = activity.findViewById(R.id.navigationCustomButton);

        assertEquals(View.GONE, customButton.getVisibility());
    }

    @Test
    public void customButtonTogglesSelectedSettingAndIcon() {
        Application context = ApplicationProvider.getApplicationContext();
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.NOTIFICATIONS);
        TestNavigationActivity activity = activity();
        ImageButton customButton = activity.findViewById(R.id.navigationCustomButton);

        assertEquals(View.VISIBLE, customButton.getVisibility());
        assertEquals(R.drawable.ic_custom_notifications_enabled, imageResource(customButton));
        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(activity));

        customButton.performClick();

        assertFalse(AppNotificationSettings.areNavigationNotificationsEnabled(activity));
        assertEquals(R.drawable.ic_custom_notifications_disabled, imageResource(customButton));
        assertEquals(
                activity.getString(
                        R.string.format_action_toggle_custom_button_setting,
                        activity.getString(R.string.label_navigation_notifications_enabled),
                        activity.getString(R.string.label_setting_disabled)
                ),
                customButton.getContentDescription().toString()
        );
    }

    @Test
    @Config(sdk = 26)
    public void surroundingStreetsTargetEnablesAfterLegacyStorageGrant() {
        installBRouterPackage();
        Application context = ApplicationProvider.getApplicationContext();
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.SURROUNDING_STREETS);
        TestNavigationActivity activity = activity();
        View customButton = activity.findViewById(R.id.navigationCustomButton);

        customButton.performClick();

        PermissionsRequest request = shadowOf(activity).getLastRequestedPermission();
        assertEquals(NavigationCustomButtonController.REQUEST_SURROUNDING_STREETS_STORAGE, request.requestCode);
        assertEquals(LEGACY_STORAGE_PERMISSION, request.requestedPermissions[0]);
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));

        activity.onRequestPermissionsResult(
                NavigationCustomButtonController.REQUEST_SURROUNDING_STREETS_STORAGE,
                new String[]{LEGACY_STORAGE_PERMISSION},
                new int[]{PERMISSION_GRANTED}
        );

        assertTrue(AppCompassSettings.isSurroundingStreetsEnabled(activity));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void surroundingStreetsTargetRequestsSegmentsTreeOnAndroid11() {
        installBRouterPackage();
        Application context = ApplicationProvider.getApplicationContext();
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.SURROUNDING_STREETS);
        TestNavigationActivity activity = activity();
        View customButton = activity.findViewById(R.id.navigationCustomButton);

        customButton.performClick();

        continueFromBRouterPrompt(activity);

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(NavigationCustomButtonController.REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE, started.requestCode);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, started.intent.getAction());
        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    public void surroundingStreetsTargetShowsBRouterRequiredToastWhenBRouterIsMissing() {
        Application context = ApplicationProvider.getApplicationContext();
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.SURROUNDING_STREETS);
        TestNavigationActivity activity = activity();
        View customButton = activity.findViewById(R.id.navigationCustomButton);

        customButton.performClick();

        assertFalse(AppCompassSettings.isSurroundingStreetsEnabled(activity));
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        assertEquals(
                activity.getString(R.string.msg_surrounding_streets_brouter_required),
                ShadowToast.getTextOfLatestToast()
        );
    }

    private static TestNavigationActivity activity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        return controller.get();
    }

    private static int imageResource(ImageButton button) {
        return ReflectionHelpers.getField(button, "mResource");
    }

    private static void continueFromBRouterPrompt(TestNavigationActivity activity) {
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

    private static void installBRouterPackage() {
        Application context = ApplicationProvider.getApplicationContext();
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowPackageManager.installPackage(packageInfo);
    }

    public static class TestNavigationActivity extends NavigationActivity {
        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return false;
        }
    }
}
