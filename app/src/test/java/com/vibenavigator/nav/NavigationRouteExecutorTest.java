package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.DeadObjectException;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.brouter.BRouterRouteException;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class NavigationRouteExecutorTest {

    @Test
    public void requestRouteDeliversSuccessfulResult() throws Exception {
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (context, start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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
                (context, start, intermediates, destination, profile, blocked) -> new GeoJsonRoute(
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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
                (context, start, intermediates, destination, profile, blocked) -> {
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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
                (context, start, intermediates, destination, profile, blocked) -> {
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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
    public void requestRouteRetriesDeadObjectFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        NavigationRouteExecutor executor = new NavigationRouteExecutor(
                (context, start, intermediates, destination, profile, blocked) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new DeadObjectException();
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            appliedRoute.set(newRoute);
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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
                (context, start, intermediates, destination, profile, blocked) -> {
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
                    ApplicationProvider.getApplicationContext(),
                    routeSnapshot(),
                    new NavigationRouteExecutor.Callback() {
                        @Override
                        public void onRouteApplied(
                                NavigationSession.RouteRequestSnapshot snapshot,
                                GeoJsonRoute newRoute,
                                long beganAt
                        ) {
                            latch.countDown();
                        }

                        @Override
                        public void onRouteFailure(
                                NavigationSession.RouteRequestSnapshot snapshot,
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

    private static NavigationSession.RouteRequestSnapshot routeSnapshot() {
        return new NavigationSession.RouteRequestSnapshot(
                1,
                1,
                new LatLon(48.2082, 16.3738),
                Collections.emptyList(),
                new LatLon(48.2100, 16.3800),
                "trekking",
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
                android.content.Context context,
                LatLon start,
                java.util.List<LatLon> intermediates,
                LatLon destination,
                String profile,
                java.util.List<com.vibenavigator.brouter.NogoPoint> blocked
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
}
