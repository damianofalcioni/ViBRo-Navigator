package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static void awaitProvider(CountDownLatch provider) {
        try {
            provider.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
