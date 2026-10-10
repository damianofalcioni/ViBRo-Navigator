package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Activity;
import android.content.Intent;
import android.provider.DocumentsContract;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.AndroidOutputBrowserActivity;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AboutOutputFolderBrowserTest {
    @Test
    public void opensSystemBrowserWithoutUsingTheBrokenFolderViewer() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            for (Kind kind : Kind.values()) {
                AndroidOutputStorageBrowser.open(activity, AndroidOutputStorage.current(activity, kind));
                org.robolectric.shadows.ShadowActivity.IntentForResult launch =
                        shadowOf(activity).getNextStartedActivityForResult();
                assertEquals(-1, launch.requestCode);
                Intent browser = launch.intent;
                assertEquals(AndroidOutputBrowserActivity.class.getName(), browser.getComponent().getClassName());
                assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, browser.getFlags());
                assertEquals("primary:Download/ViBRo/" + (kind == Kind.GPX ? "gpx" : "logs"),
                        DocumentsContract.getDocumentId(browser.getData()));
                assertNull(browser.getType());
            }
        }
    }

    @Test
    public void openingStandaloneBrowserLeavesStorageSelectionUnchanged() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            String selection = AndroidOutputStorage.current(activity, Kind.GPX).token();
            AndroidOutputStorageBrowser.open(activity, AndroidOutputStorage.current(activity, Kind.GPX));
            assertEquals(selection, AndroidOutputStorage.current(activity, Kind.GPX).token());
        }
    }
}
