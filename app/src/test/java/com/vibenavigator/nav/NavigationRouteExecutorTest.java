package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

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
}
