package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetVisibility;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

public class SurroundingStreetOverlayControllerTest {
    private final MutableClock clock = new MutableClock();
    private final CountingChunkLoader chunkLoader = new CountingChunkLoader();
    private final MutableSettings settings = new MutableSettings();
    private CountDownLatch stateLatch;
    private SurroundingStreetOverlayController controller;

    @Before
    public void setUp() {
        stateLatch = new CountDownLatch(1);
        controller = new SurroundingStreetOverlayController(
                runnable -> runnable.run(),
                clock,
                new SurroundingStreetOverlayRuntime(chunkLoader, settings),
                () -> stateLatch.countDown()
        );
    }

    @After
    public void tearDown() {
        controller.shutdown();
    }

    @Test
    public void cachedChunkPreventsReadOnEveryAcceptedFix() throws InterruptedException {
        controller.onAcceptedLocation(location(48.2082d, 16.3738d));
        controller.onCompassViewport(compassState());
        assertStateEmitted();

        int initialCalls = chunkLoader.calls;
        assertTrue(initialCalls > 1);
        assertEquals(initialCalls, controller.currentOverlay().segments.size());

        clock.nowMs = 10_000L;
        controller.onAcceptedLocation(location(48.2083d, 16.3739d));

        assertEquals(initialCalls, chunkLoader.calls);
        assertEquals(initialCalls, controller.currentOverlay().segments.size());
    }

    @Test
    public void speedBucketChangeFiltersCachedOverlayWithoutReloadingChunks() throws InterruptedException {
        chunkLoader.streetType = CompassStreetType.FOOTWAY;

        controller.onAcceptedLocation(location(48.2082d, 16.3738d));
        controller.onCompassViewport(compassStateForSpeedKmh(20f));
        assertStateEmitted();

        int initialCalls = chunkLoader.calls;
        assertEquals(initialCalls, controller.currentOverlay().segments.size());

        controller.onCompassViewport(compassStateForSpeedKmh(90f));

        assertEquals(initialCalls, chunkLoader.calls);
        assertTrue(controller.currentOverlay().isEmpty());
    }

    @Test
    public void headingOnlyViewportChangeReusesSelectionAndOverlay() throws InterruptedException {
        controller.onAcceptedLocation(location(48.2082d, 16.3738d));
        controller.onCompassViewport(compassState(1f, 0f));
        assertStateEmitted();
        CompassStreetOverlay initialOverlay = controller.currentOverlay();

        controller.onCompassViewport(compassState(1f, 90f));

        assertSame(initialOverlay, controller.currentOverlay());
    }

    @Test
    public void changedTypeSelectionInvalidatesChunksAndReloadsWithNewFilter() throws InterruptedException {
        chunkLoader.streetType = CompassStreetType.RAILWAY;
        controller.onAcceptedLocation(location(48.2082d, 16.3738d));
        NavCompassState viewport = compassState();
        controller.onCompassViewport(viewport);
        assertStateEmitted();
        int initialCalls = chunkLoader.calls;
        assertFalse(controller.currentOverlay().isEmpty());

        settings.visibility = new CompassStreetVisibility(
                EnumSet.noneOf(CompassStreetCategory.class),
                EnumSet.of(CompassStreetType.RAILWAY)
        );
        stateLatch = new CountDownLatch(1);
        controller.onCompassViewport(viewport);
        assertStateEmitted();
        assertTrue(controller.currentOverlay().isEmpty());
        assertTrue(chunkLoader.calls > initialCalls);

        int filteredCalls = chunkLoader.calls;
        settings.visibility = CompassStreetVisibility.all();
        stateLatch = new CountDownLatch(1);
        controller.onCompassViewport(viewport);
        assertStateEmitted();
        assertFalse(controller.currentOverlay().isEmpty());
        assertTrue(chunkLoader.calls > filteredCalls);
    }

    @Test
    public void routeAheadPrefetchStartsAfterVisibleChunkLoad() throws InterruptedException {
        stateLatch = new CountDownLatch(2);

        controller.onAcceptedLocation(location(0.0d, 0.0d));
        controller.onCompassViewport(routeCompassState());
        assertStateEmitted();

        assertEquals(0, stateLatch.getCount());
        assertTrue(chunkLoader.calls > 1);
    }

    @Test
    public void closingViewportInterruptsLoadAndReopeningCanLoadAgain() throws InterruptedException {
        chunkLoader.block = true;
        controller.onAcceptedLocation(location(48.2082d, 16.3738d));
        controller.onCompassViewport(compassState());
        assertTrue(chunkLoader.started.await(3, TimeUnit.SECONDS));

        controller.clearCompassViewport();

        assertTrue(chunkLoader.interrupted.await(3, TimeUnit.SECONDS));
        assertTrue(controller.currentOverlay().isEmpty());
        assertEquals(1, stateLatch.getCount());
        chunkLoader.block = false;
        controller.onCompassViewport(compassState());
        assertStateEmitted();
        assertFalse(controller.currentOverlay().isEmpty());
    }

    private void assertStateEmitted() throws InterruptedException {
        if (!stateLatch.await(3, TimeUnit.SECONDS)) {
            throw new AssertionError("Street overlay state was not emitted");
        }
    }

    private static NavCompassState compassState() {
        return compassState(1f);
    }

    private static NavCompassState compassStateForSpeedKmh(float speedKmh) {
        return compassState(speedKmh / 3.6f);
    }

    private static NavCompassState compassState(float referenceSpeedMps) {
        return compassState(referenceSpeedMps, 0f);
    }

    private static NavCompassState compassState(float referenceSpeedMps, float headingDegrees) {
        return NavCompassState.fromProjectedPoints(
                headingDegrees,
                null,
                referenceSpeedMps,
                120f,
                5f,
                true,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0f,
                0f,
                true
        );
    }

    private static NavCompassState routeCompassState() {
        return NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                120f,
                10_000f,
                120f,
                5f,
                true,
                0f,
                routeGeometry(),
                0.0d,
                0.0d,
                1,
                0f,
                0f,
                5f,
                true
        );
    }

    private static CompassRouteGeometry routeGeometry() {
        List<CompassRouteGeometry.SamplePoint> samples = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            double meters = i * 500.0d;
            samples.add(new CompassRouteGeometry.SamplePoint(
                    new LatLon(meters / 111_320.0d, 0.0d),
                    meters
            ));
        }
        return new CompassRouteGeometry(samples, Collections.emptyList());
    }

    private static NavigationLocation location(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs = 1_000L;

        @Override
        public long elapsedRealtimeMs() {
            return nowMs;
        }
    }

    private static final class CountingChunkLoader implements SurroundingStreetOverlayRuntime.ChunkLoader {
        private int calls;
        private CompassStreetType streetType = CompassStreetType.OTHER;
        private volatile boolean block;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);

        @NonNull
        @Override
        public SurroundingStreetChunkLoadResult load(
                @NonNull List<SurroundingStreetChunkKey> keys,
                @NonNull CompassStreetVisibility visibility
        ) {
            SurroundingStreetChunkLoadResult result = new SurroundingStreetChunkLoadResult();
            if (awaitCancellation()) {
                return result;
            }
            for (SurroundingStreetChunkKey key : keys) {
                LatLon center = key.center();
                calls++;
                result.put(key, visibility.isVisible(streetType)
                        ? overlayFor(center)
                        : CompassStreetOverlay.EMPTY);
            }
            return result;
        }

        private boolean awaitCancellation() {
            if (!block) {
                return false;
            }
            started.countDown();
            try {
                new CountDownLatch(1).await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
            }
            return true;
        }

        @NonNull
        private CompassStreetOverlay overlayFor(@NonNull LatLon center) {
            CompassStreetSegment segment = new CompassStreetSegment(Arrays.asList(
                    new LatLon(center.lat, center.lon),
                    new LatLon(center.lat + 0.0001d, center.lon)
            ), streetType);
            return new CompassStreetOverlay(Collections.singletonList(segment));
        }
    }

    private static final class MutableSettings implements SurroundingStreetOverlayRuntime.Settings {
        private CompassStreetVisibility visibility = CompassStreetVisibility.all();

        @Override
        public boolean isSurroundingStreetsEnabled() {
            return true;
        }

        @NonNull
        @Override
        public CompassStreetVisibility streetVisibility() {
            return visibility;
        }
    }
}
