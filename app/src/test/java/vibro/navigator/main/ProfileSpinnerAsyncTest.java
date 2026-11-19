package vibro.navigator.main;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ProfileSpinnerAsyncTest.QueuedProfiles.class)
public class ProfileSpinnerAsyncTest {
    private static final String EXTERNAL = "external";
    private static final String BUNDLED = "trekking";

    @After
    public void resetLookups() {
        QueuedProfiles.tasks.clear();
    }

    @Test
    public void startupCoalescesRefreshAndPreservesExternalChoiceUntilItCanBeRestored() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            Activity host = activity.get();
            var repository = new Profiles();
            repository.saveSelectedProfileKey(host, EXTERNAL);
            var spinner = controller(host, repository);
            spinner.refresh();
            spinner.refresh();

            assertEquals(EXTERNAL, repository.getSelectedProfileKey(host));
            assertNull(spinner.resolveSelectedProfileSelection());
            assertEquals(1, QueuedProfiles.tasks.size());
            finishLookup();
            assertEquals(EXTERNAL, spinner.resolveSelectedProfileSelection().profileName);
            assertEquals(0, QueuedProfiles.tasks.size());
            spinner.dispose();
        }
    }

    @Test
    public void explicitlyActivatingProvisionalBundledRowCommitsIt() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            Activity host = activity.get();
            var repository = new Profiles();
            repository.saveSelectedProfileKey(host, EXTERNAL);
            var widget = new RepeatSelectSpinner(host);
            var spinner = new ProfileSpinnerController(host, widget, repository, () -> { });
            spinner.refresh();

            widget.setSelection(0);
            finishLookup();

            assertEquals(BUNDLED, repository.getSelectedProfileKey(host));
            assertEquals(BUNDLED, spinner.resolveSelectedProfileSelection().profileName);
            spinner.dispose();
        }
    }

    @Test
    public void newCustomSelectionRoutesAndPersistsWhileExternalLookupIsPending() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            Activity host = activity.get();
            var repository = new Profiles();
            var spinner = controller(host, repository);
            spinner.refresh();
            finishLookup();
            spinner.refresh();

            repository.saveCustomProfile(host, Uri.parse("content://example/my-profile.brf"), "my-profile");
            spinner.onCustomProfileSaved();

            assertEquals("my-profile", spinner.resolveSelectedProfileSelection().profileName);
            assertTrue(spinner.resolveSelectedProfileSelection().customProfile);
            assertEquals(ProfileSpinnerOption.CUSTOM_KEY, repository.getSelectedProfileKey(host));
            spinner.dispose();

            var recreated = controller(host, repository);
            recreated.refresh();
            assertEquals("my-profile", recreated.resolveSelectedProfileSelection().profileName);
            recreated.dispose();
        }
    }

    @Test
    public void newBundledNamesAreShownAlongsideCachedExternalNames() {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var options = new ProfileSpinnerOptions();
            options.replaceFromLoad(activity.get(), List.of("old", EXTERNAL), List.of("old"), true, null);
            options.replaceFromLoad(activity.get(), List.of("new"), List.of("new"), false, null);

            assertEquals(List.of(EXTERNAL, "new"), options.profilesForLog());
        }
    }

    private static ProfileSpinnerController controller(Activity host, Profiles repository) {
        return new ProfileSpinnerController(host, new RepeatSelectSpinner(host), repository, () -> { });
    }

    private static void finishLookup() {
        QueuedProfiles.tasks.removeFirst().run();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Implements(className = "vibro.navigator.android.brouter.AndroidBRouterFolderDiscovery", isInAndroidSdk = false)
    public static class QueuedProfiles {
        private static final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Implementation
        protected static void executeBundledLookup(Runnable task) {
            task.run();
        }

        @Implementation
        protected static void postResult(Runnable task) {
            task.run();
        }

        @Implementation
        protected static void executeProfileLookup(Runnable task) {
            tasks.add(task);
        }
    }

    private static final class Profiles extends BRouterProfilesRepository {
        Profiles() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public boolean isBRouterInstalled(Context context) {
            return true;
        }

        @Override
        public List<String> listBundledProfiles(Context context) {
            return List.of(BUNDLED);
        }

        @Override
        public List<String> listAvailableProfiles(Context context, List<String> bundled,
                                                  androidx.core.util.Consumer<List<String>> progress) {
            return List.of(EXTERNAL, BUNDLED);
        }
    }
}
