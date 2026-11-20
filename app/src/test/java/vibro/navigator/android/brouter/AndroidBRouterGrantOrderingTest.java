package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowContentResolver;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterFolderGrant;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterGrantOrderingTest {
    @Test
    @Config(shadows = DelayFirstGrant.class)
    public void lateValidationDoesNotReplaceANewerGrant() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri earlier = createParent("earlier");
            Uri later = createParent("later");
            AndroidBRouterFolderAccess.saveSelection(activity, grant(earlier), ignored -> { });
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(later)));
            assertEquals(later, BRouterFolderGrant.get(activity));

            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));

            assertEquals(later, BRouterFolderGrant.get(activity));
        }
    }

    @Test
    @Config(shadows = BlockFirstGrant.class)
    public void aTimedOutQueryDoesNotLockAnotherTreeFromTheSameProvider() throws InterruptedException {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            String authority = "test.brouter.multiple";
            var provider = new BlockingTreeProvider();
            ShadowContentResolver.registerProviderInternal(authority, provider);
            Uri slow = DocumentsContract.buildTreeDocumentUri(authority, "slow");
            Uri healthy = DocumentsContract.buildTreeDocumentUri(authority, "root");
            AtomicReference<Boolean> result = new AtomicReference<>();
            try {
                AndroidBRouterFolderAccess.saveSelection(activity, grant(slow), result::set);
                assertTrue(provider.started.await(5, TimeUnit.SECONDS));
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));
                assertEquals(false, result.get());

                assertTrue(TestBRouterStorage.saveSelection(activity, grant(healthy)));
            } finally {
                provider.release.countDown();
                BlockFirstGrant.worker.join(5000);
            }
        }
    }

    @Test
    @Config(shadows = DelayFirstGrant.class)
    public void aLateSegmentsOnlyGrantComplementsTheNewerProfilesGrant() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri earlier = createChild("slow-parent", "segments4");
            Uri later = createChild("new-parent", "profiles2");
            var accepted = new AtomicReference<Boolean>();
            AndroidBRouterFolderAccess.saveSelection(activity, grant(earlier), accepted::set);
            assertTrue(TestBRouterStorage.saveSelection(activity, grant(later)));

            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));

            assertEquals(Boolean.TRUE, accepted.get());
            assertEquals(earlier, BRouterFolderGrant.parentFor(activity, false));
            assertEquals(later, BRouterFolderGrant.parentFor(activity, true));
        }
    }

    @Test
    public void streetOnlyGrantStaysScopedUntilTheParentIsSelectedForSharedAccess() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri parent = createParent("tiles");
            var saved = new AtomicReference<Boolean>();
            AndroidBRouterFolderAccess.saveStreetSelection(activity, grant(parent), saved::set);
            assertEquals(Boolean.TRUE, saved.get());

            AndroidBRouterFolderAccess.recheckSharedFolder(activity, parent);
            assertNull(BRouterFolderGrant.directory(activity, parent, true));

            assertTrue(TestBRouterStorage.saveSelection(activity, grant(parent)));
            assertTrue(AndroidBRouterProfilesRepositoryFactory.create().hasPersistedProfilesTreeAccess(activity));
        }
    }

    private static Uri createChild(String directory, String child) {
        String parent = directory + "/brouter";
        TestBRouterStorage.createPrimaryDirectory(parent + "/" + child);
        return AndroidDocumentAccess.buildExternalStorageTreeUri("primary:" + parent);
    }

    private static Uri createParent(String directory) {
        String parent = directory + "/brouter";
        TestBRouterStorage.createPrimaryDirectory(parent + "/profiles2");
        TestBRouterStorage.createPrimaryDirectory(parent + "/segments4");
        return AndroidDocumentAccess.buildExternalStorageTreeUri("primary:" + parent);
    }

    private static Intent grant(Uri tree) {
        return new Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    @Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
    public static class DelayFirstGrant {
        private static boolean first = true;

        @Implementation
        protected static void execute(Runnable work) {
            if (first) {
                first = false;
                new Handler(Looper.getMainLooper()).postDelayed(work, 600L);
            } else {
                work.run();
            }
        }
    }

    @Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
    public static class BlockFirstGrant {
        private static Thread worker;

        @Implementation
        protected static void execute(Runnable work) {
            if (worker == null) {
                worker = new Thread(work, "test provider lookup");
                worker.setDaemon(true);
                worker.start();
            } else {
                work.run();
            }
        }
    }

    private static final class BlockingTreeProvider extends AndroidBRouterParentGrantTest.ParentProvider {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
            if ("slow".equals(DocumentsContract.getTreeDocumentId(uri))) {
                awaitRelease();
            }
            return super.query(uri, projection, selection, args, sort);
        }

        private void awaitRelease() {
            started.countDown();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // This provider ignores cancellation for one tree only.
                }
            }
        }
    }
}
