package vibro.navigator.android.brouter;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AndroidBRouterFolderDiscoveryTest {
    @Test
    public void probesExecuteOutsideTheCallingThread() throws InterruptedException {
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        CountDownLatch complete = new CountDownLatch(1);
        AndroidBRouterFolderDiscovery.execute(() -> {
            worker.set(Thread.currentThread());
            complete.countDown();
        });

        assertTrue(complete.await(3, TimeUnit.SECONDS));
        assertNotSame(caller, worker.get());
    }
}
