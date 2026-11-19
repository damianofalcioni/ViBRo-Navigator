package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    public void slowCandidateCannotHideALocalFallback() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            var activity = controller.get();
            TestBRouterStorage.createPrimaryDirectory("brouter/segments4");
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            TestBRouterStorage.registerDirectoryProvider(defaultParent(), () -> waitForRelease(started, release));
            AtomicReference<Uri> result = new AtomicReference<>();
            try {
                AndroidBRouterSegmentsTreeAccessPrompt.findAvailableFolder(activity, Collections.emptyList(), result::set);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));
                assertEquals(AndroidDocumentAccess.buildExternalStorageDocumentUri("primary:brouter"), result.get());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    public void validationTimeoutPreservesTheAlreadyVerifiedChild() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            var activity = controller.get();
            TestBRouterStorage.createPrimaryFolder("profiles2");
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            TestBRouterStorage.registerDirectoryProvider(defaultParent(), () -> waitForRelease(started, release));
            AtomicReference<Boolean> accepted = new AtomicReference<>();
            try {
                Intent grant = new Intent().setData(defaultParent()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                AndroidBRouterFolderAccess.saveSelection(activity, grant, accepted::set);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));

                assertEquals(true, accepted.get());
                assertTrue(AndroidBRouterProfilesRepositoryFactory.create().hasPersistedProfilesTreeAccess(activity));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    public void profileProviderTimeoutCannotHideReadableSegments() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri uri = android.provider.DocumentsContract.buildTreeDocumentUri("test.child.documents", "root");
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var provider = new AndroidBRouterParentGrantTest.ParentProvider() {
                @Override
                public android.database.Cursor query(Uri document, String[] projection, String selection,
                                                     String[] args, String sort) {
                    String id = android.provider.DocumentsContract.getDocumentId(document);
                    if ("profile-dir".equals(id)) {
                        waitForRelease(started, release);
                    }
                    return super.query(document, projection, selection, args, sort);
                }
            };
            org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(uri.getAuthority(), provider);
            var verified = new AtomicReference<>(AndroidBRouterDirectoryCheck.Directories.empty());
            var completed = new AtomicReference<AndroidBRouterDirectoryCheck.Directories>();
            try {
                AndroidBRouterFolderDiscovery.findWithFallback(
                        signal -> AndroidBRouterDirectoryCheck.verify(activity, uri, signal, verified),
                        verified::get, completed::set);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                awaitVerifiedSegments(verified);
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));
                assertEquals(verified.get().segments(), completed.get().segments());
            } finally {
                release.countDown();
            }
        }
    }

    private static void awaitVerifiedSegments(AtomicReference<AndroidBRouterDirectoryCheck.Directories> verified)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (verified.get().segments().uri() == null && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(verified.get().segments().uri() != null);
    }

    @Test
    @Config(shadows = ResumeDiscovery.class)
    public void mainScreenUsesTheRunningSearchAfterPauseAndResume() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            var info = new PackageInfo();
            info.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
            shadowOf(activity.getPackageManager()).installPackage(info);
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            TestBRouterStorage.registerDirectoryProvider(defaultParent(), () -> {
                waitForRelease(started, release);
                return true;
            });
            var setup = new AndroidBRouterStorageSetup(activity, null);
            setup.bindManualAction(new View(activity), () -> { });
            try {
                setup.setForegroundChecksEnabled(true);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                setup.setForegroundChecksEnabled(false);
                controller.pause().stop().start().resume();
                setup.setForegroundChecksEnabled(true);

                release.countDown();
                ResumeDiscovery.worker.join(5000);
                shadowOf(Looper.getMainLooper()).idle();

                assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
            } finally {
                release.countDown();
                setup.dispose();
            }
        }
    }

    @Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
    public static class ResumeDiscovery {
        private static Thread worker;

        @org.robolectric.annotation.Implementation
        protected static void execute(Runnable work) {
            if (worker == null) {
                worker = new Thread(work, "test folder discovery");
                worker.setDaemon(true);
                worker.start();
            } else {
                work.run();
            }
        }

        @org.robolectric.annotation.Implementation
        protected static void postResult(Runnable work) {
            new android.os.Handler(Looper.getMainLooper()).post(work);
        }
    }

    private static Uri defaultParent() {
        return AndroidDocumentAccess.buildExternalStorageTreeUri("primary:Android/media/btools.routingapp/brouter");
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
