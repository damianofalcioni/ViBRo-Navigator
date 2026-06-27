package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class SurroundingStreetOverlayControllerTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final MutableClock clock = new MutableClock();
    private final CountingRepository repository = new CountingRepository();
    private CountDownLatch stateLatch;
    private SurroundingStreetOverlayController controller;

    @Before
    public void setUp() {
        AppCompassSettings.setSurroundingStreetsEnabled(context, false);
        AppCompassSettings.setSurroundingStreetsEnabled(context, true);
        stateLatch = new CountDownLatch(1);
        controller = new SurroundingStreetOverlayController(
                context,
                runnable -> runnable.run(),
                clock,
                repository,
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

        assertEquals(1, repository.calls);
        assertEquals(1, controller.currentOverlay().segments.size());

        clock.nowMs = 10_000L;
        controller.onAcceptedLocation(location(48.2083d, 16.3739d));

        assertEquals(1, repository.calls);
        assertEquals(1, controller.currentOverlay().segments.size());
    }

    @Test
    public void routeAheadPrefetchStartsAfterVisibleChunkLoad() throws InterruptedException {
        stateLatch = new CountDownLatch(2);

        controller.onAcceptedLocation(location(0.0d, 0.0d));
        controller.onCompassViewport(routeCompassState());
        assertStateEmitted();

        assertEquals(0, stateLatch.getCount());
        assertTrue(repository.calls > 1);
    }

    private void assertStateEmitted() throws InterruptedException {
        if (!stateLatch.await(3, TimeUnit.SECONDS)) {
            throw new AssertionError("Street overlay state was not emitted");
        }
    }

    private static NavCompassState compassState() {
        return NavCompassState.fromProjectedPoints(
                0f,
                null,
                1f,
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

    private static final class CountingRepository implements SurroundingStreetRepository {
        private int calls;

        @NonNull
        @Override
        public CompassStreetOverlay loadSurroundingStreets(
                @NonNull Context context,
                double latitude,
                double longitude,
                double radiusMeters,
                int maxSegments
        ) {
            calls++;
            return new CompassStreetOverlay(Collections.singletonList(new CompassStreetSegment(Arrays.asList(
                    new LatLon(latitude, longitude),
                    new LatLon(latitude + 0.0001d, longitude)
            ))));
        }
    }
}
