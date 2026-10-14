package vibro.navigator.nav.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

public class NavigationRouteExecutorShutdownTest {
    @Test(timeout = 10_000L)
    public void shutdownReturnsWhileUninterruptibleRouteIsRunningAndClosesOnceInBackground() throws Exception {
        BlockingCalculator calculator = new BlockingCalculator();
        AtomicInteger callbacks = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(calculator,
                Executors.newSingleThreadExecutor(), Runnable::run);
        try {
            executor.requestRoute(new NavigationRouteRequestSnapshot(1, 1,
                    new LatLon(0, 0), Collections.emptyList(), new LatLon(0, 0.01),
                    "trekking", null, Collections.emptyList()), callbackCounter(callbacks));
            assertTrue(calculator.started.await(3, TimeUnit.SECONDS));

            executor.shutdown();
            executor.shutdown();

            assertEquals(1, calculator.closed.getCount());
            assertEquals(0, callbacks.get());
        } finally {
            calculator.release.release();
            executor.shutdown();
        }
        assertTrue(calculator.closed.await(3, TimeUnit.SECONDS));
        assertEquals(1, calculator.closeCount.get());
        assertNotEquals(Thread.currentThread(), calculator.closeThread);
        assertEquals(0, callbacks.get());
    }

    private static NavigationRouteExecutor.Callback callbackCounter(AtomicInteger callbacks) {
        return new NavigationRouteExecutor.Callback() {
            @Override
            public void onRouteApplied(NavigationRouteRequestSnapshot snapshot, GeoJsonRoute route, long beganAt) {
                callbacks.incrementAndGet();
            }

            @Override
            public void onRouteFailure(NavigationRouteRequestSnapshot snapshot, Exception error) {
                callbacks.incrementAndGet();
            }
        };
    }

    private static final class BlockingCalculator implements NavigationRouteExecutor.RouteCalculator, AutoCloseable {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final Semaphore release = new Semaphore(0);
        private final AtomicInteger closeCount = new AtomicInteger();
        private Thread closeThread;

        @Override
        public GeoJsonRoute routeGeoJson(LatLon start, List<LatLon> intermediates, LatLon destination,
                String profile, List<NogoPoint> blocked) {
            started.countDown();
            release.acquireUninterruptibly();
            return new GeoJsonRoute(Arrays.asList(start, destination), Collections.emptyList(), 42, 120);
        }

        @Override
        public void close() {
            closeThread = Thread.currentThread();
            closeCount.incrementAndGet();
            closed.countDown();
        }
    }
}
