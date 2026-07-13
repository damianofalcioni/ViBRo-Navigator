package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionGpxExportTest {
    private static final String DESTINATION = "Destination";
    private static final String GPX_ROUTE_POINT = "<rtept ";
    private static final String GPX_TRACK_POINT = "<trkpt ";
    private static final String GPX_WAYPOINT = "<wpt ";
    private static final String TYPE_DESTINATION = "vibro.navigator.destination";
    private static final String TYPE_STOP = "vibro.navigator.stop";
    private static final String TYPE_TURN = "vibro.navigator.turn";
    private static final String TYPE_GPS_FIX = "vibro.navigator.gps-fix";
    private static final String FIRST_GPS_FIX_TIME = "<time>1970-01-01T00:00:01.000Z</time>";
    private static final String FIRST_GPS_FIX_ELEVATION = "<ele>188.5</ele>";

    @Test
    public void buildCurrentRouteGpx_exportsStraightLinePointsAndStraightRouteOnly() {
        Context androidContext = ApplicationProvider.getApplicationContext();
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        LatLon stop = new LatLon(0.0, 0.001);
        LatLon destination = new LatLon(0.0, 0.002);
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                destination,
                Collections.singletonList(stop)
        ));
        long nowMs = 1_000L;

        assertTrue(NavigationSessionResourceAdapter.start(session, textResources, nowMs));
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeedAndAltitude(0.0, 0.0, nowMs, 2f, 188.5),
                nowMs
        );
        String gpx = session.buildCurrentRouteGpx(androidContext);

        assertNotNull(gpx);
        assertEquals(3, countOccurrences(gpx, GPX_ROUTE_POINT));
        assertEquals(3, countOccurrences(gpx, GPX_TRACK_POINT));
        assertEquals(3, countOccurrences(gpx, GPX_WAYPOINT));
        assertTrue(gpx.contains(TYPE_STOP));
        assertTrue(gpx.contains(TYPE_DESTINATION));
        assertTrue(gpx.contains(TYPE_GPS_FIX));
        assertTrue(gpx.contains(FIRST_GPS_FIX_TIME));
        assertTrue(gpx.contains(FIRST_GPS_FIX_ELEVATION));
        assertFalse(gpx.contains(TYPE_TURN));
    }

    @Test
    public void buildCurrentRouteGpx_exportsStraightLineAcceptedFixPathAsPassedTrack() {
        Context androidContext = ApplicationProvider.getApplicationContext();
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                DESTINATION,
                new LatLon(0.0, 0.01),
                Collections.emptyList()
        ));

        assertTrue(NavigationSessionResourceAdapter.start(session, textResources, 1_000L));
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeed(0.0, 0.0, 1_000L, 2f),
                1_000L
        );
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeed(0.0, 0.001, 2_000L, 2f),
                2_000L
        );

        String gpx = session.buildCurrentRouteGpx(androidContext);

        assertNotNull(gpx);
        assertEquals(2, countOccurrences(gpx, "<trkseg>"));
        assertEquals(4, countOccurrences(gpx, GPX_TRACK_POINT));
        assertEquals(2, countOccurrences(gpx, TYPE_GPS_FIX));
        assertTrue(gpx.contains("Passed route"));
    }

    @Test
    public void buildCurrentRouteGpx_exportsPassedRouteHistoryAndAcceptedFixes() {
        Context androidContext = ApplicationProvider.getApplicationContext();
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                "trekking",
                DESTINATION,
                new LatLon(0.0, 0.003),
                Collections.emptyList()
        ));

        assertTrue(NavigationSessionResourceAdapter.start(session, textResources, 1_000L));
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeed(0.0, 0.0, 1_000L, 2f),
                1_000L
        );
        NavigationRouteRequestSnapshot firstSnapshot = session.prepareRouteRequest(true, 1_000L);
        assertNotNull(firstSnapshot);
        NavigationSessionResourceAdapter.applyRouteResult(
                session,
                textResources,
                firstSnapshot,
                route(new VoiceHint(0, 2, 0, 40.0, -90), 0.0, 0.0, 0.001, 0.002),
                1_000L
        );
        NavigationSessionResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeed(0.0, 0.002, 2_000L, 2f),
                2_000L
        );
        NavigationRouteRequestSnapshot secondSnapshot = session.prepareRouteRequest(true, 3_000L);
        assertNotNull(secondSnapshot);
        NavigationSessionResourceAdapter.applyRouteResult(
                session,
                textResources,
                secondSnapshot,
                route(new VoiceHint(1, 5, 0, 30.0, 90), 0.0, 0.0025, 0.00275, 0.003),
                3_000L
        );

        String gpx = session.buildCurrentRouteGpx(androidContext);

        assertNotNull(gpx);
        assertEquals(3, countOccurrences(gpx, "<trkseg>"));
        assertTrue(countOccurrences(gpx, GPX_TRACK_POINT) >= 5);
        assertEquals(5, countOccurrences(gpx, GPX_WAYPOINT));
        assertTrue(gpx.contains("Passed route"));
        assertTrue(gpx.contains("Turn left"));
        assertTrue(gpx.contains("Turn right"));
        assertTrue(gpx.contains(TYPE_GPS_FIX));
        assertTrue(gpx.contains(FIRST_GPS_FIX_TIME));
    }

    @NonNull
    private static NavigationLocation locationWithSpeed(
            double lat,
            double lon,
            long timeMs,
            float speedMetersPerSecond
    ) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        location.setSpeed(speedMetersPerSecond);
        return location;
    }

    @NonNull
    private static NavigationLocation locationWithSpeedAndAltitude(
            double lat,
            double lon,
            long timeMs,
            float speedMetersPerSecond,
            double altitudeMeters
    ) {
        NavigationLocation location = locationWithSpeed(lat, lon, timeMs, speedMetersPerSecond);
        location.setAltitude(altitudeMeters);
        return location;
    }

    @NonNull
    private static GeoJsonRoute route(
            @NonNull VoiceHint hint,
            double startLat,
            double startLon,
            double middleLon,
            double endLon
    ) {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(startLat, startLon),
                        new LatLon(startLat, middleLon),
                        new LatLon(startLat, endLon)
                ),
                Collections.singletonList(hint),
                60.0,
                333.0
        );
    }

    private static int countOccurrences(@NonNull String value, @NonNull String pattern) {
        int count = 0;
        int offset = 0;
        while (offset >= 0) {
            offset = value.indexOf(pattern, offset);
            if (offset >= 0) {
                count++;
                offset += pattern.length();
            }
        }
        return count;
    }
}
