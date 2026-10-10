package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AndroidOutputPermissionRequestTest {
    private final Application app = ApplicationProvider.getApplicationContext();
    private final List<Boolean> results = new ArrayList<>();

    @Before
    public void setUp() {
        AppOutputStorageSettings.disable(app);
        AndroidOutputStorage.resetFailures();
        AppGpxSettings.setAutoSaveOnStopEnabled(app, false);
        shadowOf(app).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        CardActivity.media = null;
    }

    @Test
    public void bothKindsRequestMissingPhonePermissionAndDenialDoesNotCompleteSuccessfully() {
        for (Kind kind : Kind.values()) {
            Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
            AndroidOutputPermissionRequest access = request(activity);
            access.request(kind);
            ShadowActivity.PermissionsRequest permission = shadowOf(activity).getLastRequestedPermission();
            assertNotNull(permission);
            assertArrayEquals(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, permission.requestedPermissions);
            assertTrue(results.isEmpty());
            access.handlePermissionResult(permission.requestCode);
            assertEquals(List.of(false), results);
            results.clear();
        }
    }

    @Test
    public void manualGpxGrantResumesAfterRecreationWithoutEnablingAutoSave() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidOutputPermissionRequest original = request(activity);
        original.request(Kind.GPX);
        int code = shadowOf(activity).getLastRequestedPermission().requestCode;
        Bundle state = new Bundle();
        original.saveState(state);
        Activity replacement = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidOutputPermissionRequest restored = request(replacement);
        restored.restoreState(state);
        assertNull(shadowOf(replacement).getLastRequestedPermission());
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        assertTrue(restored.handlePermissionResult(code));
        assertEquals(List.of(true), results);
        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
    }

    @Test
    public void cancellingPendingRequestIgnoresLaterPhoneGrant() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidOutputPermissionRequest access = request(activity);
        access.request(Kind.LOGS);
        int code = shadowOf(activity).getLastRequestedPermission().requestCode;
        access.cancel(Kind.LOGS);
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        assertTrue(access.handlePermissionResult(code));
        assertTrue(results.isEmpty());
    }

    @Test
    public void retainedSdGrantSkipsPickerAndRevokedGrantRequestsSelectedCard() throws IOException {
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        CardActivity.media = card.media;
        CardActivity activity = Robolectric.buildActivity(CardActivity.class).setup().get();
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(app,
                "com.android.externalstorage.documents", TestOutputStorageCard.ID + ":Download");
        android.net.Uri tree = provider.tree(TestOutputStorageCard.ID + ":Download");
        AppOutputStorageSettings.select(app, TestOutputStorageCard.ID, null);
        AndroidOutputPermissionRequest access = request(activity);
        access.request(Kind.GPX);
        assertEquals(List.of(true), results);
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        results.clear();
        app.getContentResolver().releasePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        access.request(Kind.GPX);
        assertTrue(results.isEmpty());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowActivity.IntentForResult picker = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.intent.getAction());
        Bundle state = new Bundle();
        access.saveState(state);
        CardActivity replacement = Robolectric.buildActivity(CardActivity.class).setup().get();
        AndroidOutputPermissionRequest restored = request(replacement);
        restored.restoreState(state);
        Intent grant = new Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        assertTrue(restored.handleActivityResult(picker.requestCode, Activity.RESULT_OK, grant));
        assertEquals(List.of(true), results);
        assertFalse(AppGpxSettings.isAutoSaveOnStopEnabled(app));
    }

    @Test
    public void cancelledSdExplanationCannotLaunchPickerOrCompleteAgain() {
        CardActivity.media = new TestOutputStorageCard(app).media;
        CardActivity activity = Robolectric.buildActivity(CardActivity.class).setup().get();
        AppOutputStorageSettings.select(app, TestOutputStorageCard.ID, null);
        AndroidOutputPermissionRequest access = request(activity);
        access.request(Kind.LOGS);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        access.cancel(Kind.LOGS);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        dialog.cancel();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(results.isEmpty());
    }

    private AndroidOutputPermissionRequest request(Activity activity) {
        return new AndroidOutputPermissionRequest(activity, (kind, allowed) -> results.add(allowed), Runnable::run);
    }

    public static class CardActivity extends Activity {
        static File media;

        @Override
        public File[] getExternalMediaDirs() {
            return media == null ? super.getExternalMediaDirs() : new File[]{TestOutputMediaDirs.primary(this), media};
        }
    }
}
