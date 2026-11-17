package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = AndroidBRouterDiscoveryRecoveryTest.RealDiscovery.class)
public class AndroidBRouterDiscoveryRecoveryTest {
    @Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
    public static class RealDiscovery { }

    @Test
    public void timedOutLookupsAreCancelledAndHealthyLookupsStillRun() throws InterruptedException {
        var started = new CountDownLatch(2);
        var cancelled = new CountDownLatch(2);
        var provider = new CountDownLatch(1);
        var results = new AtomicInteger();
        try {
            for (int i = 0; i < 2; i++) {
                AndroidBRouterFolderDiscovery.find(signal -> {
                    signal.setOnCancelListener(cancelled::countDown);
                    started.countDown();
                    awaitProvider(provider);
                    return 100;
                }, 1, results::addAndGet);
            }
            assertTrue(started.await(5, TimeUnit.SECONDS));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
            assertEquals(2, results.get());

            var healthy = new CountDownLatch(1);
            AndroidBRouterFolderDiscovery.find(signal -> {
                healthy.countDown();
                return true;
            }, false, ignored -> { });
            assertTrue(healthy.await(5, TimeUnit.SECONDS));
        } finally {
            provider.countDown();
        }
    }

    @Test
    public void repeatedLookupsDoNotPileUpBehindAStuckProvider() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var tree = AndroidDocumentAccess.buildExternalStorageTreeUri("primary:stuck");
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            TestBRouterStorage.registerDirectoryProvider(tree, () -> waitForRelease(started, release));
            try {
                probe(activity, () -> { });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
                for (int i = 0; i < 8; i++) {
                    var passedProvider = new CountDownLatch(1);
                    probe(activity, passedProvider::countDown);
                    assertTrue(passedProvider.await(2, TimeUnit.SECONDS));
                }
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    @Config(shadows = AndroidBRouterSegmentsTreeAccessPromptTest.DelayedDiscovery.class)
    public void manualBrowseContinuesWhenMainScreenResumes() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            PackageInfo info = new PackageInfo();
            info.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
            shadowOf(activity.getPackageManager()).installPackage(info);
            TestBRouterStorage.requireGrantToQueryPrimaryFolder(activity);
            var setup = new AndroidBRouterStorageSetup(activity, null);
            View action = new View(activity);
            setup.bindManualAction(action, () -> { });
            setup.setForegroundChecksEnabled(true);
            action.performClick();

            setup.setForegroundChecksEnabled(false);
            controller.pause().stop();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800));
            controller.start().resume();
            setup.setForegroundChecksEnabled(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800));

            assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
            setup.dispose();
        }
    }

    private static void probe(Activity activity, Runnable afterProvider) {
        AndroidBRouterFolderDiscovery.find(signal -> {
            DirectoryPresence result = AndroidDocumentAccess.externalStorageDirectoryPresence(activity, "primary:stuck", signal);
            afterProvider.run();
            return result;
        }, DirectoryPresence.UNKNOWN, ignored -> { });
    }

    private static boolean waitForRelease(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        while (release.getCount() != 0) {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                // Simulate a provider that does not return when the caller interrupts the query.
            }
        }
        return false;
    }

    private static void awaitProvider(CountDownLatch provider) {
        try {
            provider.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
