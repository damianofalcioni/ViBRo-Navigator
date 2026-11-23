package vibro.navigator.android.brouter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;

import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.android.storage.AndroidDocumentAccess;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = AndroidBRouterDiscoveryRecoveryTest.RealDiscovery.class)
public class AndroidBRouterProfileLoaderTest {
    private static final String BUNDLED = "trekking";
    private static final String HEALTHY = "healthy-external";
    private static final String MEDIA_ID = "primary:Android/media/btools.routingapp/brouter";
    @Test
    public void profileEnumerationRunsOffTheMainThread() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var repository = new RecordingRepository();
            var loader = new AndroidBRouterProfileLoader(activity.get(), repository, ignored -> { });
            try {
                loader.refresh();

                drainMainUntil(() -> repository.enumerated.getCount() == 0);
                assertFalse(repository.onMainThread.get());
            } finally {
                loader.dispose();
            }
        }
    }

    @Test
    public void healthyDiscoveryRunsWhileTwoAbandonedScansAreStillStalled() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var stalled = new StalledRepository();
            var first = new AndroidBRouterProfileLoader(activity.get(), stalled, ignored -> { });
            var second = new AndroidBRouterProfileLoader(activity.get(), stalled, ignored -> { });
            var delivered = new AtomicReference<List<String>>();
            var fresh = new AndroidBRouterProfileLoader(activity.get(), new RecordingRepository(), result -> {
                if (result.complete()) {
                    delivered.set(result.profiles());
                }
            });
            try {
                first.refresh();
                second.refresh();
                drainMainUntil(() -> stalled.started.getCount() == 0);
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));
                fresh.refresh();
                drainMainUntil(() -> delivered.get() != null);
                assertEquals(List.of(HEALTHY), delivered.get());
            } finally {
                stalled.release.countDown();
                first.dispose();
                second.dispose();
                fresh.dispose();
            }
        }
    }

    @Test
    public void profilesAlreadyDiscoveredSurviveTheNextProviderTimingOut() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var repository = new StalledRepository();
            var delivered = new AtomicReference<List<String>>();
            var loader = new AndroidBRouterProfileLoader(activity.get(), repository, result -> delivered.set(result.profiles()));
            try {
                loader.refresh();
                drainMainUntil(() -> repository.started.getCount() < 2);
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));

                assertEquals(List.of(BUNDLED, HEALTHY), delivered.get());
            } finally {
                repository.release.countDown();
                loader.dispose();
            }
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void pickerKeepsLaterReadableLocalFolderWhenEarlierProviderStalls() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            String local = "Android/data/btools.routingapp/files/brouter/profiles2";
            TestBRouterStorage.createPrimaryDirectory(local);
            var blocked = new BlockingQuery();
            Uri early = AndroidDocumentAccess.buildExternalStorageTreeUri(MEDIA_ID + "/profiles2");
            TestBRouterStorage.registerDirectoryProvider(early, blocked::waitForRelease);
            var location = new AtomicReference<Uri>();
            try {
                AndroidBRouterProfilesTreeAccessPrompt.findCustomProfileLocation(activity.get(),
                        AndroidBRouterProfilesRepositoryFactory.create(), location::set);
                assertTrue(blocked.entered.await(5, TimeUnit.SECONDS));
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));

                assertEquals(AndroidDocumentAccess.buildExternalStorageDocumentUri("primary:" + local), location.get());
            } finally {
                blocked.release.countDown();
            }
        }
    }

    @Test
    public void unannotatedPreferredProfileResolvesOffMainWithoutQueryingOlderGrant() throws Exception {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var directory = TestBRouterStorage.createPrimaryDirectory(MEDIA_ID.substring("primary:".length()) + "/profiles2");
            Files.writeString(directory.toPath().resolve(BUNDLED + ".brf"), "assign validForBikes = true");
            Uri parent = AndroidDocumentAccess.buildExternalStorageTreeUri(MEDIA_ID);
            Uri earlier = AndroidDocumentAccess.buildExternalStorageTreeUri("primary:retained/profiles2");
            activity.getContentResolver().takePersistableUriPermission(parent, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.getContentResolver().takePersistableUriPermission(earlier, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            var repository = AndroidBRouterProfilesRepositoryFactory.create();
            repository.saveProfilesTreeUri(activity, earlier);
            BRouterFolderGrant.save(activity, parent, AndroidDocumentAccess.descendantDocumentUri(parent, MEDIA_ID + "/profiles2"), null);
            var blocked = new BlockingQuery();
            TestBRouterStorage.registerDirectoryProvider(earlier, blocked::waitForRelease);
            var parameters = new AtomicReference<List<BRouterProfileParameter>>();
            try {
                AndroidBRouterProfileLoader.loadParameters(activity, repository, BUNDLED, false, parameters::set);
                drainMainUntil(() -> parameters.get() != null || blocked.entered.getCount() == 0);

                assertEquals(1L, blocked.entered.getCount());
                assertEquals(List.of(), parameters.get());
            } finally {
                blocked.release.countDown();
            }
        }
    }

    private static final class BlockingQuery {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        boolean waitForRelease() {
            entered.countDown();
            try {
                return release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    @Test
    public void customPickerInitialLocationIsResolvedOffTheUiThread() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var called = new CountDownLatch(1);
            var mainThread = new AtomicBoolean();
            var repository = new BRouterProfilesRepository(BRouterProfileTestDependencies.create()) {
                @Override
                public android.net.Uri getCustomProfilePickerInitialUri(Context context) {
                    mainThread.set(Looper.getMainLooper().isCurrentThread());
                    called.countDown();
                    return null;
                }
            };
            AndroidBRouterProfilesTreeAccessPrompt.findCustomProfileLocation(activity.get(), repository, ignored -> { });
            assertTrue(called.await(5, TimeUnit.SECONDS));
            assertFalse(mainThread.get());
        }
    }

    private static void drainMainUntil(androidx.core.util.Supplier<Boolean> done) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!done.get() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
        assertTrue(done.get());
    }

    private static final class StalledRepository extends BRouterProfilesRepository {
        private final CountDownLatch started = new CountDownLatch(2);
        private final CountDownLatch release = new CountDownLatch(1);

        StalledRepository() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public List<String> listBundledProfiles(Context context) {
            return List.of(BUNDLED);
        }

        @Override
        public List<String> listAvailableProfiles(Context context, List<String> bundled,
                                                  androidx.core.util.Consumer<List<String>> progress) {
            progress.accept(List.of(BUNDLED, HEALTHY));
            started.countDown();
            while (release.getCount() != 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // A document provider may ignore interruption.
                }
            }
            return bundled;
        }
    }

    private static final class RecordingRepository extends BRouterProfilesRepository {
        final CountDownLatch enumerated = new CountDownLatch(1);
        final AtomicBoolean onMainThread = new AtomicBoolean();

        RecordingRepository() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public List<String> listAvailableProfiles(Context context, List<String> bundled,
                                                  androidx.core.util.Consumer<List<String>> progress) {
            onMainThread.set(Looper.getMainLooper().isCurrentThread());
            enumerated.countDown();
            return List.of(HEALTHY);
        }
    }
}
