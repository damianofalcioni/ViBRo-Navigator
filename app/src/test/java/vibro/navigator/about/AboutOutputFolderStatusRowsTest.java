package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputFolderAccess;
import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
public class AboutOutputFolderStatusRowsTest {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ActivityController<Activity> controller;
    private Activity activity;
    private AboutOutputFolderStatusRows rows;
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        AndroidAppTheme.apply(activity);
        activity.setContentView(R.layout.about_diagnostics_section);
        provider = TestOutputDocumentsProvider.install(activity);
        AppOutputFolderSettings.set(activity, Kind.LOGS, null);
        AppOutputFolderSettings.set(activity, Kind.GPX, null);
        rows = new AboutOutputFolderStatusRows(activity, worker);
    }

    @After
    public void tearDown() {
        rows.shutdown();
        controller.close();
    }

    @Test
    public void rowsAreHiddenWhenDefaultFoldersAreUsed() {
        rows.render();
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionLogFolderRow).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionGpxFolderRow).getVisibility());
    }

    @Test
    public void accessibleCustomFoldersShowGreenAndIndependentRepairPickers() throws Exception {
        selectBoth();
        rows.render();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, true);
        assertStatus(R.id.aboutPermissionGpxFolderStatus, true);
        assertEquals(View.VISIBLE, activity.findViewById(R.id.aboutPermissionLogFolderRow).getVisibility());
        activity.findViewById(R.id.aboutPermissionLogFolderRow).performClick();
        ShadowActivity.IntentForResult logs = shadowOf(activity).getNextStartedActivityForResult();
        activity.findViewById(R.id.aboutPermissionGpxFolderRow).performClick();
        ShadowActivity.IntentForResult gpx = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, logs.intent.getAction());
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, gpx.intent.getAction());
        assertTrue((logs.intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
        assertNotEquals(logs.requestCode, gpx.requestCode);
    }

    @Test
    public void writeFailuresShowRedEvenWithPersistedGrants() throws Exception {
        selectBoth();
        provider.failWrites = true;
        AndroidOutputFolderAccess.markUnavailable(activity, Kind.LOGS);
        AndroidOutputFolderAccess.markUnavailable(activity, Kind.GPX);
        rows.render();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, false);
        assertStatus(R.id.aboutPermissionGpxFolderStatus, false);
        provider.failWrites = false;
        rows.render();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, true);
        assertStatus(R.id.aboutPermissionGpxFolderStatus, true);
    }

    @Test
    public void permissionRevocationShowsRedAndResetHidesRow() throws Exception {
        selectBoth();
        activity.getContentResolver().releasePersistableUriPermission(TestOutputDocumentsProvider.TREE,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        rows.render();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, false);
        AppOutputFolderSettings.set(activity, Kind.LOGS, null);
        rows.render();
        finishCheck();
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionLogFolderRow).getVisibility());
    }

    @Test
    public void stoppedChecksCannotUpdateOldRows() throws Exception {
        selectBoth();
        rows.render();
        rows.stop();
        finishCheck();
        assertStatus(R.id.aboutPermissionLogFolderStatus, false);
    }

    private void selectBoth() {
        AppOutputFolderSettings.set(activity, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
        AppOutputFolderSettings.set(activity, Kind.GPX, TestOutputDocumentsProvider.TREE.toString());
    }

    private void finishCheck() throws Exception {
        worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void assertStatus(int id, boolean allowed) {
        TextView status = activity.findViewById(id);
        assertEquals(activity.getString(allowed ? R.string.permission_status_ok
                : R.string.permission_status_needs_attention), status.getText().toString());
        assertEquals(ContextCompat.getColor(activity, allowed ? R.color.success : R.color.danger),
                status.getCurrentTextColor());
    }
}
