package vibro.navigator.android.brouter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = AndroidBRouterDiscoveryRecoveryTest.RealDiscovery.class)
public class AndroidBRouterProfileLoaderTest {
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
    public void bundledProfilesAreAvailableWhenBothExternalWorkersAreStalled() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var stalled = new StalledRepository();
            var first = new AndroidBRouterProfileLoader(activity.get(), stalled, ignored -> { });
            var second = new AndroidBRouterProfileLoader(activity.get(), stalled, ignored -> { });
            var delivered = new java.util.concurrent.atomic.AtomicReference<List<String>>();
            var fresh = new AndroidBRouterProfileLoader(activity.get(), stalled, result -> delivered.set(result.profiles()));
            try {
                first.refresh();
                second.refresh();
                drainMainUntil(() -> stalled.started.getCount() == 0);
                org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1100));
                fresh.refresh();
                drainMainUntil(() -> delivered.get() != null);
                org.junit.Assert.assertEquals(List.of("trekking"), delivered.get());
            } finally {
                stalled.release.countDown();
                first.dispose();
                second.dispose();
                fresh.dispose();
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

    private static void drainMainUntil(java.util.function.BooleanSupplier done) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!done.getAsBoolean() && System.nanoTime() < deadline) {
            org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
        assertTrue(done.getAsBoolean());
    }

    private static final class StalledRepository extends BRouterProfilesRepository {
        private final CountDownLatch started = new CountDownLatch(2);
        private final CountDownLatch release = new CountDownLatch(1);

        StalledRepository() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public List<String> listBundledProfiles(Context context) {
            return List.of("trekking");
        }

        @Override
        public List<String> listAvailableProfiles(Context context, List<String> bundled) {
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
        public List<String> listAvailableProfiles(Context context, List<String> bundled) {
            onMainThread.set(Looper.getMainLooper().isCurrentThread());
            enumerated.countDown();
            return Collections.emptyList();
        }
    }
}
