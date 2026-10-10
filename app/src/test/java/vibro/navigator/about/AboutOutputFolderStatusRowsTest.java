package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Activity;
import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.TestOutputMediaDirs;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AboutOutputFolderStatusRowsTest {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ActivityController<Activity> controller;
    private Activity activity;
    private AboutOutputFolderStatusRows rows;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        AndroidAppTheme.apply(activity);
        activity.setContentView(R.layout.about_diagnostics_section);
        AppOutputStorageSettings.disable(activity);
        AndroidOutputStorage.resetFailures();
        rows = new AboutOutputFolderStatusRows(activity, worker);
    }

    @After
    public void tearDown() {
        rows.shutdown();
        controller.close();
    }

    @Test
    @Config(sdk = {29, 35})
    public void mediaStoreFoldersAreHiddenWithoutSchedulingAccessChecks() {
        worker.shutdown();
        rows.render();
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionLogFolderRow).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionGpxFolderRow).getVisibility());
    }

    @Test
    public void legacyFoldersRemainVisibleWithoutSavingEnabledAndShowMissingPermission() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).denyPermissions("android.permission.WRITE_EXTERNAL_STORAGE");
        rows.render();
        finishCheck();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.aboutPermissionLogFolderRow).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.aboutPermissionGpxFolderRow).getVisibility());
        assertFolderNames();
        assertStatus(R.id.aboutPermissionGpxFolderStatus, false);
    }

    @Test
    public void permissionGrantRefreshesBothFoldersToDownloads() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).grantPermissions("android.permission.WRITE_EXTERNAL_STORAGE");
        rows.render();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, true);
        assertStatus(R.id.aboutPermissionGpxFolderStatus, true);
        assertFolderNames();
    }

    @Test
    public void stoppedChecksCannotUpdateOldRows() throws Exception {
        rows.render();
        rows.stop();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, false);
    }

    private void finishCheck() throws Exception {
        worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void assertStatus(int id, boolean allowed) {
        assertEquals(activity.getString(allowed ? R.string.permission_status_ok
                : R.string.permission_status_needs_attention),
                ((TextView) activity.findViewById(id)).getText().toString());
    }

    private void assertFolderNames() {
        assertEquals(activity.getString(R.string.title_log_output_folder),
                ((TextView) activity.findViewById(R.id.aboutPermissionLogFolderLabel)).getText().toString());
        assertEquals(activity.getString(R.string.title_gpx_output_folder),
                ((TextView) activity.findViewById(R.id.aboutPermissionGpxFolderLabel)).getText().toString());
    }
}
