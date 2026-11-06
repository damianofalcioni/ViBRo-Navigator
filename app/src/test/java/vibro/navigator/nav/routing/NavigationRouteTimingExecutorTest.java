package vibro.navigator.nav.routing;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import static org.junit.Assert.*;

public class NavigationRouteTimingExecutorTest {
    @Test
    public void executorUsesInjectedMonotonicClockAndSeparatesQueuedMainThreadDelivery() throws Exception {
        Clock clock = new Clock();
        Scheduler scheduler = new Scheduler();
        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(1, 1, new LatLon(0, 0),
                Collections.emptyList(), new LatLon(0, .001), "trekking", null, Collections.emptyList());
        NavigationRouteExecutor executor = new NavigationRouteExecutor((start, stops, destination, profile, blocked) -> {
            clock.nowMs += 700;
            return new GeoJsonRoute(Arrays.asList(start, destination), Collections.emptyList(), 100, 100);
        }, Executors.newSingleThreadExecutor(), scheduler, calculation -> calculation.call(), clock);
        try {
            executor.requestRoute(snapshot, new NavigationRouteExecutor.Callback() {
                @Override public void onRouteApplied(NavigationRouteRequestSnapshot request, GeoJsonRoute route, long beganAt) {
                    assertEquals(5_000_000, beganAt);
                    request.timing.applicationStarted();
                    clock.nowMs += 40;
                }
                @Override public void onRouteFailure(NavigationRouteRequestSnapshot request, Exception error) {
                    throw new AssertionError(error);
                }
            });
            assertTrue(scheduler.posted.await(2, TimeUnit.SECONDS));
            clock.nowMs += 6_300;
            scheduler.pending.run();
            assertEquals("calculationMs=700 callbackWaitMs=6300 confirmationWaitMs=0 applicationMs=40 totalMs=7040",
                    snapshot.timing.applicationCompleted());
        } finally {
            executor.shutdown();
        }
    }

    private static final class Clock implements ElapsedRealtimeClock {
        private volatile long nowMs = 5_000_000;
        @Override public long elapsedRealtimeMs() { return nowMs; }
    }

    private static final class Scheduler implements TaskScheduler {
        private final CountDownLatch posted = new CountDownLatch(1);
        private Runnable pending;
        @Override public void post(Runnable runnable) {
            pending = runnable;
            posted.countDown();
        }
    }
}
