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

import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;

@RunWith(RobolectricTestRunner.class)
public class NavigationSessionGpxExportTest {
    private static final String DESTINATION = "Destination";
    private static final String GPX_ROUTE_POINT = "<rtept ";
    private static final String GPX_TRACK_POINT = "<trkpt ";
    private static final String GPX_WAYPOINT = "<wpt ";
    private static final String TYPE_DESTINATION = "vibro.navigator.destination";
    private static final String TYPE_STOP = "vibro.navigator.stop";
    private static final String TYPE_TURN = "vibro.navigator.turn";

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

        assertTrue(NavigationSession.ResourceAdapter.start(session, textResources, nowMs));
        NavigationSession.ResourceAdapter.onRawLocationChanged(
                session,
                textResources,
                locationWithSpeed(0.0, 0.0, nowMs, 2f),
                nowMs
        );
        String gpx = session.buildCurrentRouteGpx(androidContext);

        assertNotNull(gpx);
        assertEquals(3, countOccurrences(gpx, GPX_ROUTE_POINT));
        assertEquals(3, countOccurrences(gpx, GPX_TRACK_POINT));
        assertEquals(2, countOccurrences(gpx, GPX_WAYPOINT));
        assertTrue(gpx.contains(TYPE_STOP));
        assertTrue(gpx.contains(TYPE_DESTINATION));
        assertFalse(gpx.contains(TYPE_TURN));
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
