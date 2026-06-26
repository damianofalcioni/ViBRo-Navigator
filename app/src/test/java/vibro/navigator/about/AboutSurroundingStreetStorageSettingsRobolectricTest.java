package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppCompassSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity.PermissionsRequest;
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
        ShadowToast.reset();
    }

    @Test
    public void surroundingStreetsSwitchPersistsAfterLegacyStorageGrant() {
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
    public void diagnosticShowsLegacyStreetStorageStatusWhenSurroundingStreetsNeedIt() {
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
    public void diagnosticHidesLegacyStreetStorageStatusWhenSurroundingStreetsAreOff() {
        AboutActivity activity = activity();
        idleInitialDiagnosticRender();

        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionSurroundingStreetStorageRow).getVisibility());
    }

    private static AboutActivity activity() {
        return Robolectric.buildActivity(AboutActivity.class).setup().get();
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
                100,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }
}
