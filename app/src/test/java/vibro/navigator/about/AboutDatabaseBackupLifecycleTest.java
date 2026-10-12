package vibro.navigator.about;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayDeque;
import java.util.Queue;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidOutputBrowserActivity;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AboutDatabaseBackupLifecycleTest {
    @Test
    public void existingAboutReceivesImportThroughNewIntentWithoutStartingAnotherScreen() {
        try (ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            AboutActivity activity = controller.get();
            Queue<Runnable> work = new ArrayDeque<>();
            AboutDatabaseBackupActions actions = new AboutDatabaseBackupActions(activity, () -> { }, () -> { }, work::add);
            ReflectionHelpers.setField(activity, "databaseBackupActions", actions);
            Intent returned = selectedBackup(activity);
            controller.newIntent(returned);
            assertEquals(1, work.size());
            assertFalse(AndroidOutputStorageBrowser.hasResult(returned));
            controller.newIntent(returned);
            assertEquals(1, work.size());
            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test
    public void restoredAboutDoesNotReplayImportFromItsOriginalSystemLaunchIntent() {
        Intent returned;
        try (ActivityController<AboutActivity> caller = AboutActivityTestSupport.setupControllerWithSettings()) {
            returned = selectedBackup(caller.get());
        }
        // Android can retain the original result extras in the system task record after process death.
        try (ActivityController<AboutActivity> restored =
                     Robolectric.buildActivity(AboutActivity.class, returned).setup(new Bundle())) {
            AboutDatabaseBackupActions actions = ReflectionHelpers.getField(restored.get(), "databaseBackupActions");
            assertFalse(ReflectionHelpers.<Boolean>getField(actions, "busy"));
            assertNull(shadowOf(restored.get()).getNextStartedActivity());
        }
    }

    private static Intent selectedBackup(AboutActivity caller) {
        AndroidOutputStorageBrowser.openForResult(caller, AndroidDocumentAccess.openDocumentIntent(null), 4002);
        Intent launch = shadowOf(caller).getNextStartedActivityForResult().intent;
        shadowOf(caller).getNextStartedActivity();
        try (ActivityController<AndroidOutputBrowserActivity> browser =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, launch).setup()) {
            IntentForResult picker = shadowOf(browser.get()).getNextStartedActivityForResult();
            shadowOf(browser.get()).getNextStartedActivity();
            shadowOf(browser.get()).receiveResult(picker.intent, Activity.RESULT_OK,
                    new Intent().setData(Uri.parse("content://backups/document/backup.json")));
            return shadowOf(browser.get()).getNextStartedActivity();
        }
    }
}
