package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
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
import java.io.IOException;
import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.TestOutputDocumentsProvider;
import vibro.navigator.android.storage.TestOutputMediaDirs;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppOutputStorageSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AboutOutputFolderStatusRowsTest {
    private static final String WRITE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String CARD_ID = "ABCD-1234";
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
    public void mediaStorePermissionRowIsHiddenWithoutSchedulingAccessChecks() {
        worker.shutdown();
        rows.render();
        assertEquals(View.GONE, activity.findViewById(R.id.aboutPermissionOutputFolderRow).getVisibility());
    }

    @Test
    public void legacyPermissionRowRemainsVisibleWithoutSavingEnabled() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).denyPermissions(WRITE_PERMISSION);
        rows.render();
        finishCheck();
        assertEquals(View.VISIBLE, activity.findViewById(R.id.aboutPermissionOutputFolderRow).getVisibility());
        assertFolderName();
        assertStatus(false);
        activity.findViewById(R.id.aboutPermissionOutputFolderRow).performClick();
        assertEquals(WRITE_PERMISSION, shadowOf(activity).getLastRequestedPermission().requestedPermissions[0]);
    }

    @Test
    public void permissionGrantAndRevocationRefreshSharedStatus() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).grantPermissions(WRITE_PERMISSION);
        rows.render();
        finishCheck();
        assertStatus(true);
        assertFolderName();
        shadowOf(app).denyPermissions(WRITE_PERMISSION);
        rows.render();
        finishCheck();
        assertStatus(false);
    }

    @Test
    public void outputWriteFailureDoesNotChangeGrantedPermission() throws Exception {
        shadowOf((Application) activity.getApplicationContext()).grantPermissions(WRITE_PERMISSION);
        AndroidOutputStorage.failed(Kind.GPX, AndroidOutputStorage.current(activity, Kind.GPX));
        rows.render();
        finishCheck();
        assertStatus(true);
    }

    @Test
    public void sdAncestorGrantsCoverAllOutputsWithoutCreatingFolders() throws Exception {
        for (String path : new String[]{"", "Download", "Download/ViBRo"}) {
            TestOutputDocumentsProvider provider = selectSdGrant(CARD_ID + ":" + path);
            rows.render();
            finishCheck();
            assertStatus(true);
            assertEquals(0, provider.names().length);
            provider.unavailable = true;
            rows.render();
            finishCheck();
            assertStatus(true);
            activity.getContentResolver().releasePersistableUriPermission(provider.tree(CARD_ID + ":" + path),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            rows.render();
            finishCheck();
            assertStatus(false);
        }
    }

    @Test
    public void sdGrantNeedsBothReadAndWritePermissions() throws Exception {
        TestOutputDocumentsProvider provider = selectSdGrant(CARD_ID + ":Download");
        activity.getContentResolver().releasePersistableUriPermission(provider.tree(CARD_ID + ":Download"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        rows.render();
        finishCheck();
        assertStatus(false);
    }

    @Test
    public void unrelatedAndChildFolderGrantsDoNotAuthorizeSharedOutputFolder() throws Exception {
        selectSdGrant("OTHER-1234:Download");
        selectSdGrant(CARD_ID + ":Download/ViBRo/logs");
        shadowOf((Application) activity.getApplicationContext()).grantPermissions(WRITE_PERMISSION);
        rows.render();
        finishCheck();
        assertStatus(false);
    }

    @Test
    public void stoppedChecksCannotUpdateOldRows() throws Exception {
        shadowOf((Application) activity.getApplicationContext()).grantPermissions(WRITE_PERMISSION);
        rows.render();
        rows.stop();
        finishCheck();
        assertStatus(false);
    }

    private TestOutputDocumentsProvider selectSdGrant(String documentId) throws IOException {
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(activity,
                "com.android.externalstorage.documents", documentId);
        Uri tree = provider.tree(documentId);
        AppOutputStorageSettings.select(activity, CARD_ID, tree.toString());
        return provider;
    }

    private void finishCheck() throws Exception {
        worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void assertStatus(boolean allowed) {
        assertEquals(activity.getString(allowed ? R.string.permission_status_ok
                : R.string.permission_status_needs_attention),
                ((TextView) activity.findViewById(R.id.aboutPermissionOutputFolderStatus)).getText().toString());
    }

    private void assertFolderName() {
        assertEquals(activity.getString(R.string.label_permission_output_folder),
                ((TextView) activity.findViewById(R.id.aboutPermissionOutputFolderLabel)).getText().toString());
    }
}
