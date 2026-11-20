package vibro.navigator.main;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.ArrayDeque;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterStorageSetup;
import vibro.navigator.android.brouter.TestBRouterStorage;
import vibro.navigator.android.storage.AndroidOutputBrowserActivity;
import vibro.navigator.android.storage.AndroidOutputStorageBrowser;
import vibro.navigator.android.storage.TestDownloadsProvider;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MainActivityCustomProfileTaskTest {
    private static final String LATER_PROFILE = "later";
    private BRouterProfilesRepository profiles;
    private Uri profile;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AppMainUiSettings.completeWelcome(context);
        AppMainUiSettings.setRoutingMode(context, NavigationRoutingMode.ROUND_TRIP);
        PackageInfo brouter = new PackageInfo();
        brouter.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(context.getPackageManager()).installPackage(brouter);
        profiles = AndroidBRouterProfilesRepositoryFactory.create();
        ContentValues metadata = new ContentValues();
        metadata.put("_display_name", "custom.brf");
        metadata.put("mime_type", "application/octet-stream");
        profile = TestDownloadsProvider.install(context).insert(
                Uri.parse("content://media/external_primary/downloads"), metadata);
    }

    @Test
    public void customActionWaitsForTheOpenAutomaticFolderPrompt() {
        verifyPickerOrder(null);
    }

    @Test
    @Config(shadows = QueuedFolderQueries.class)
    public void racingLookupsOpenOnlyTheCustomPickerInEitherCompletionOrder() {
        verifyPickerOrder(true);
        verifyPickerOrder(false);
    }

    private void verifyPickerOrder(Boolean customFirst) {
        TestBRouterStorage.createPrimaryFolder("profiles2");
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var setup = new AndroidBRouterStorageSetup(activity, null);
            var picker = new MainActivityProfilePicker(activity, profiles);
            picker.restore(null, setup::isPickerPending);
            setup.coordinatePicker(picker::isPickerRequested, picker::tryOpenPicker);
            setup.bindManualAction(new View(activity), () -> { });
            setup.setResumed(true);
            try {
                setup.setForegroundChecksEnabled(true);
                picker.startCustomProfilePicker();
                assertNull(shadowOf(activity).getNextStartedActivity());
                completeFolderLookups(customFirst);
                shadowOf(Looper.getMainLooper()).idle();
                Intent launched = shadowOf(activity).getNextStartedActivity();
                assertNotNull(launched);
                assertEquals(AndroidOutputBrowserActivity.class.getName(), launched.getComponent().getClassName());
                assertNull(shadowOf(activity).getNextStartedActivity());
            } finally {
                QueuedFolderQueries.tasks.clear();
                setup.dispose();
            }
        }
    }

    private static void completeFolderLookups(Boolean customFirst) {
        if (customFirst == null) {
            var prompt = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(prompt.isShowing());
            prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            return;
        }
        assertEquals(2, QueuedFolderQueries.tasks.size());
        var automatic = QueuedFolderQueries.tasks.removeFirst();
        var custom = QueuedFolderQueries.tasks.removeFirst();
        if (customFirst) {
            custom.run();
            automatic.run();
        } else {
            automatic.run();
            custom.run();
        }
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Implements(className = "vibro.navigator.android.brouter.AndroidBRouterFolderDiscovery", isInAndroidSdk = false)
    public static class QueuedFolderQueries {
        private static final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Implementation
        protected static void execute(Runnable work) {
            tasks.add(work);
        }

        @Implementation
        protected static void postResult(Runnable callback) {
            callback.run();
        }
    }

    @Test
    public void selectionReturnsToExistingMainWithoutChangingModeOrInputsAndIsConsumedOnce() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            EditText destination = activity.findViewById(R.id.destinationEdit);
            destination.setText("Saved destination");
            int mode = ((Spinner) activity.findViewById(R.id.routeModeSpinner)).getSelectedItemPosition();
            Intent result = pickerResult(activity, Activity.RESULT_OK, new Intent().setData(profile)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
            controller.newIntent(result);
            assertEquals("custom", profiles.getCustomProfileName(activity));
            assertEquals(profile, profiles.getCustomProfileUri(activity));
            assertEquals(ProfileSpinnerOption.CUSTOM_KEY, profiles.getSelectedProfileKey(activity));
            assertEquals(mode, ((Spinner) activity.findViewById(R.id.routeModeSpinner)).getSelectedItemPosition());
            assertEquals("Saved destination", destination.getText().toString());
            assertFalse(AndroidOutputStorageBrowser.hasResult(result));
            profiles.saveCustomProfile(activity, profile, LATER_PROFILE);
            controller.recreate();
            assertEquals(LATER_PROFILE, profiles.getCustomProfileName(controller.get()));
        }
    }

    @Test
    public void cancellationKeepsExistingCustomProfileAndNavigationMode() {
        Context context = ApplicationProvider.getApplicationContext();
        profiles.saveCustomProfile(context, profile, "previous");
        profiles.saveSelectedProfileKey(context, ProfileSpinnerOption.CUSTOM_KEY);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            controller.newIntent(pickerResult(activity, Activity.RESULT_CANCELED, null));
            assertEquals("previous", profiles.getCustomProfileName(activity));
            assertEquals(ProfileSpinnerOption.CUSTOM_KEY, profiles.getSelectedProfileKey(activity));
            assertEquals(NavigationRoutingMode.ROUND_TRIP, AppMainUiSettings.getRoutingMode(activity));
        }
    }

    @Test
    public void selectionCanReturnToNewMainActivityAndSurviveRecreationWithoutReplay() {
        Intent result;
        try (ActivityController<MainActivity> caller = Robolectric.buildActivity(MainActivity.class).setup()) {
            result = pickerResult(caller.get(), Activity.RESULT_OK, new Intent().setData(profile)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class, result).setup()) {
            assertEquals("custom", profiles.getCustomProfileName(controller.get()));
            assertEquals(ProfileSpinnerOption.CUSTOM_KEY, profiles.getSelectedProfileKey(controller.get()));
            profiles.saveCustomProfile(controller.get(), profile, LATER_PROFILE);
            controller.recreate();
            assertEquals(LATER_PROFILE, profiles.getCustomProfileName(controller.get()));
        }
    }

    private Intent pickerResult(MainActivity activity, int resultCode, Intent data) {
        new MainActivityProfilePicker(activity, profiles).startCustomProfilePicker();
        Intent hostIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(AndroidOutputBrowserActivity.class.getName(), hostIntent.getComponent().getClassName());
        try (ActivityController<AndroidOutputBrowserActivity> host =
                     Robolectric.buildActivity(AndroidOutputBrowserActivity.class, hostIntent).setup()) {
            ShadowActivity.IntentForResult picker = shadowOf(host.get()).getNextStartedActivityForResult();
            shadowOf(host.get()).getNextStartedActivity();
            shadowOf(host.get()).receiveResult(picker.intent, resultCode, data);
            assertTrue(host.get().isFinishing());
            return shadowOf(host.get()).getNextStartedActivity();
        }
    }
}
