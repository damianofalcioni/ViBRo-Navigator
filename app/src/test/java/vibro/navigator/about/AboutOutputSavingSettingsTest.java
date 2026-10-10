package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.widget.Switch;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import vibro.navigator.R;
import vibro.navigator.android.storage.TestOutputMediaDirs;
import vibro.navigator.android.storage.TestOutputPermissionActivity;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = {TestOutputMediaDirs.class, TestOutputPermissionActivity.class})
public class AboutOutputSavingSettingsTest {
    private static final String WRITE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private final Application app = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AppGpxSettings.setAutoSaveOnStopEnabled(app, false);
        AppLogger.init(app);
        AppLogger.setLoggingEnabled(app, false);
        shadowOf(app).denyPermissions(WRITE_PERMISSION);
    }

    @Test
    public void eachSwitchStaysOffAfterDenialAndEnablesOnlyItselfAfterGrant() {
        for (int id : new int[]{R.id.aboutLogEnabledSwitch, R.id.aboutAutoSaveGpxSwitch}) {
            AppLogger.setLoggingEnabled(app, false);
            AppGpxSettings.setAutoSaveOnStopEnabled(app, false);
            shadowOf(app).denyPermissions(WRITE_PERMISSION);
            AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
            Switch saving = activity.findViewById(id);
            saving.setChecked(true);
            AboutActivityTestSupport.finishOutputSaving(activity);
            ShadowActivity.PermissionsRequest permission = shadowOf(activity).getLastRequestedPermission();
            assertNotNull(permission);
            assertFalse(AppLogger.isLoggingEnabled());
            assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
            activity.onRequestPermissionsResult(permission.requestCode, permission.requestedPermissions,
                    new int[]{-1});
            AboutActivityTestSupport.finishOutputSaving(activity);
            assertFalse(saving.isChecked());
            saving.setChecked(true);
            AboutActivityTestSupport.finishOutputSaving(activity);
            permission = shadowOf(activity).getLastRequestedPermission();
            assertEquals(4100 + (id == R.id.aboutLogEnabledSwitch ? 0 : 1), permission.requestCode);
            shadowOf(app).grantPermissions(permission.requestedPermissions);
            assertFalse(vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess.needsOutputPermission(activity));
            activity.onRequestPermissionsResult(permission.requestCode, permission.requestedPermissions,
                    new int[]{0});
            AboutActivityTestSupport.finishOutputSaving(activity);
            assertEquals(id == R.id.aboutLogEnabledSwitch, AppLogger.isLoggingEnabled());
            assertEquals(id == R.id.aboutAutoSaveGpxSwitch, AppGpxSettings.isAutoSaveOnStopEnabled(app));
            assertTrue(saving.isChecked());
            activity.finish();
        }
    }
}
