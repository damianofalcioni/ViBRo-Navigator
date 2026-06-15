package vibro.navigator.nav.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.geo.LatLon;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.route.GeoJsonRoute;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NavigationRouteExecutorTest {

    @Test
    public void requestRouteDeliversSuccessfulResult() throws Exception {
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
                        Arrays.asList(start, destination),
                        Collections.emptyList(),
                        42.0,
                        120.0
                ),
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GeoJsonRoute> appliedRoute = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertNotNull(appliedRoute.get());
            assertEquals(2, appliedRoute.get().track.size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRouteTreatsEmptyTrackAsFailure() throws Exception {
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0.0,
                        0.0
                ),
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicReference<GeoJsonRoute> appliedRoute = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNull(appliedRoute.get());
            assertNotNull(failure.get());
            assertEquals("BRouter returned an empty route", failure.get().getMessage());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRouteRetriesTransientBRouterFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw BRouterRouteException.serviceUnavailable("BRouter service not available");
                    }
                    return new GeoJsonRoute(
                            Arrays.asList(start, destination),
                            Collections.emptyList(),
                            42.0,
                            120.0
                    );
                },
                Executors.newSingleThreadExecutor(),
                Runnable::run,
                1,
                0L,
                delayMs -> {
                }
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GeoJsonRoute> appliedRoute = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertNotNull(appliedRoute.get());
            assertEquals(2, attempts.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRouteFailsAfterTransientRetriesAreExhausted() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> {
                    attempts.incrementAndGet();
                    throw BRouterRouteException.serviceUnavailable("BRouter service not available");
                },
                Executors.newSingleThreadExecutor(),
                Runnable::run,
                1,
                0L,
                delayMs -> {
                }
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(failure.get());
            assertEquals("BRouter service not available", failure.get().getMessage());
            assertEquals(2, attempts.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRouteRetriesAdapterMappedBinderFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw BRouterRouteException.serviceUnavailable("BRouter binder died during route request");
                    }
                    return new GeoJsonRoute(
                            Arrays.asList(start, destination),
                            Collections.emptyList(),
                            42.0,
                            120.0
                    );
                },
                Executors.newSingleThreadExecutor(),
                Runnable::run,
                1,
                0L,
                delayMs -> {
                }
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GeoJsonRoute> appliedRoute = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertNotNull(appliedRoute.get());
            assertEquals(2, attempts.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRouteDoesNotRetryNoRouteFoundBRouterFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> {
                    attempts.incrementAndGet();
                    throw BRouterRouteException.fromTextResponse("no track found at pass=0");
                },
                Executors.newSingleThreadExecutor(),
                Runnable::run,
                1,
                0L,
                delayMs -> {
                }
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNotNull(failure.get());
            assertEquals("no track found at pass=0", failure.get().getMessage());
            assertEquals(1, attempts.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shutdownClosesClosableRouteCalculator() {
        AtomicInteger closeCount = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                new ClosableRouteCalculator(closeCount),
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );

        executor.shutdown();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void requestRouteReportsFailureWhenExecutorRejectsSubmission() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.shutdownNow();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
                        Arrays.asList(start, destination),
                        Collections.emptyList(),
                        42.0,
                        120.0
                ),
                executorService,
                Runnable::run
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();

        executor.requestRoute(
                routeSnapshot(),
                new NavigationRouteExecutor.Callback() {
                    @Override
                    public void onRouteApplied(
                            NavigationRouteRequestSnapshot snapshot,
                            GeoJsonRoute newRoute,
                            long beganAt
                    ) {
                        latch.countDown();
                    }

                    @Override
                    public void onRouteFailure(
                            NavigationRouteRequestSnapshot snapshot,
                            Exception error
                    ) {
                        failure.set(error);
                        latch.countDown();
                    }
                }
        );

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(failure.get());
        assertEquals(
                "Route calculation rejected because the executor is shut down",
                failure.get().getMessage()
        );
    }

    @Test
    public void shutdownSuppressesQueuedRouteCallbacks() throws Exception {
        QueueingScheduler scheduler = new QueueingScheduler();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
                        Arrays.asList(start, destination),
                        Collections.emptyList(),
                        42.0,
                        120.0
                ),
                Executors.newSingleThreadExecutor(),
                scheduler
        );
        AtomicInteger callbacks = new AtomicInteger();
        boolean shutdown = false;

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            callbacks.incrementAndGet();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            callbacks.incrementAndGet();
                        }
                    }
            );

            assertTrue(scheduler.awaitPost());
            executor.shutdown();
            shutdown = true;
            scheduler.runAll();

            assertEquals(0, callbacks.get());
        } finally {
            if (!shutdown) {
                executor.shutdown();
            }
        }
    }

    @Test
    public void requestRouteRunsCalculationInsideConfiguredGuard() throws Exception {
        AtomicInteger guardRuns = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
                        Arrays.asList(start, destination),
                        Collections.emptyList(),
                        42.0,
                        120.0
                ),
                Executors.newSingleThreadExecutor(),
                Runnable::run,
                routeCalculation -> {
                    guardRuns.incrementAndGet();
                    return routeCalculation.call();
                },
                0,
                0L,
                delayMs -> {
                }
        );
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();

        try {
            executor.requestRoute(
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            failure.set(error);
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertEquals(1, guardRuns.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void requestRoutePassesProfileParametersToCalculator() throws Exception {
        AtomicReference<String> receivedProfileParameters = new AtomicReference<>();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                new ProfileParameterCapturingCalculator(receivedProfileParameters),
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );
        CountDownLatch latch = new CountDownLatch(1);

        try {
            executor.requestRoute(
                    routeSnapshot("avoid_path=1"),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationRouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationRouteRequestSnapshot snapshot,
                                Exception error
                        ) {
                            latch.countDown();
                        }
                    }
            );

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals("avoid_path=1", receivedProfileParameters.get());
        } finally {
            executor.shutdown();
        }
    }

    private static final class QueueingScheduler implements TaskScheduler {
        private final Queue<Runnable> queued = new ArrayDeque<>();
        private final CountDownLatch postLatch = new CountDownLatch(1);

        @Override
        public synchronized void post(Runnable runnable) {
            queued.add(runnable);
            postLatch.countDown();
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            post(runnable);
        }

        @Override
        public synchronized void removeCallbacks(Runnable runnable) {
            queued.remove(runnable);
        }

        boolean awaitPost() throws InterruptedException {
            return postLatch.await(2, TimeUnit.SECONDS);
        }

        void runAll() {
            while (true) {
                Runnable next;
                synchronized (this) {
                    next = queued.poll();
                }
                if (next == null) {
                    return;
                }
                next.run();
            }
        }
    }

    private static NavigationRouteRequestSnapshot routeSnapshot() {
        return routeSnapshot(null);
    }

    private static NavigationRouteRequestSnapshot routeSnapshot(String profileParameters) {
        return new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(48.2082, 16.3738),
                Collections.emptyList(),
                new LatLon(48.2100, 16.3800),
                "trekking",
                profileParameters,
                Collections.emptyList()
        );
    }

    private static final class ClosableRouteCalculator
            implements NavigationRouteExecutor.RouteCalculator, AutoCloseable {

        private final AtomicInteger closeCount;

        private ClosableRouteCalculator(AtomicInteger closeCount) {
            this.closeCount = closeCount;
        }

        @Override
        public GeoJsonRoute routeGeoJson(
                LatLon start,
                java.util.List<LatLon> intermediates,
                LatLon destination,
                String profile,
                java.util.List<NogoPoint> blocked
        ) {
            return new GeoJsonRoute(
                    Arrays.asList(start, destination),
                    Collections.emptyList(),
                    42.0,
                    120.0
            );
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class ProfileParameterCapturingCalculator
            implements NavigationRouteExecutor.RouteCalculator {
        private final AtomicReference<String> receivedProfileParameters;

        private ProfileParameterCapturingCalculator(AtomicReference<String> receivedProfileParameters) {
            this.receivedProfileParameters = receivedProfileParameters;
        }

        @Override
        public GeoJsonRoute routeGeoJson(
                LatLon start,
                java.util.List<LatLon> intermediates,
                LatLon destination,
                String profile,
                java.util.List<NogoPoint> blocked
        ) {
            return new GeoJsonRoute(
                    Arrays.asList(start, destination),
                    Collections.emptyList(),
                    42.0,
                    120.0
            );
        }

        @Override
        public GeoJsonRoute routeGeoJson(
                LatLon start,
                java.util.List<LatLon> intermediates,
                LatLon destination,
                String profile,
                java.util.List<NogoPoint> blocked,
                String profileParameters
        ) {
            receivedProfileParameters.set(profileParameters);
            return routeGeoJson(start, intermediates, destination, profile, blocked);
        }
    }
}
