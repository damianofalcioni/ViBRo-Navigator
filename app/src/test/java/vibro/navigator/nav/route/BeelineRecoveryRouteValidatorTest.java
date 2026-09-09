package vibro.navigator.nav.route;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class BeelineRecoveryRouteValidatorTest {
    private static final LatLon START = new LatLon(0, 0);
    private static final LatLon STOP = new LatLon(0, 0.001);
    private static final LatLon END = new LatLon(0, 0.003);

    @Test
    public void acceptsUsableRoadWithNecessaryFinalBeelineToExactDestination() {
        GeoJsonRoute route = route(Arrays.asList(START, STOP, new LatLon(0, 0.002), END),
                Collections.singletonList(new VoiceHint(2, 16, 0, 111, 0)));
        assertTrue(usable(route, Collections.singletonList(STOP), fix(0, 0)));
    }

    @Test
    public void rejectsMissingStopWrongOrderOrChangedDestination() {
        assertFalse(usable(route(Arrays.asList(START, END), Collections.emptyList()),
                Collections.singletonList(STOP), fix(0, 0)));
        LatLon next = new LatLon(0, 0.002);
        assertFalse(usable(route(Arrays.asList(START, next, STOP, END), Collections.emptyList()),
                Arrays.asList(STOP, next), fix(0, 0)));
        assertFalse(usable(route(Arrays.asList(START, STOP), Collections.emptyList()),
                Collections.emptyList(), fix(0, 0)));
    }

    @Test
    public void rejectsSameBeelineAndDistantSnappedStreet() {
        assertFalse(usable(route(Arrays.asList(START, END),
                Collections.singletonList(new VoiceHint(0, 16, 0, 333, 0))),
                Collections.emptyList(), fix(0, 0)));
        assertFalse(usable(route(Arrays.asList(new LatLon(0.001, 0), END), Collections.emptyList()),
                Collections.emptyList(), fix(0, 0)));
    }

    @Test
    public void latestPositionMustMatchRoadBeforeTheFirstRemainingStop() {
        GeoJsonRoute route = route(Arrays.asList(START, STOP, END), Collections.emptyList());
        assertFalse(usable(route, Collections.singletonList(STOP), fix(0, 0.002)));
        assertFalse(usable(route, Collections.emptyList(), fix(0.001, 0)));
        NavigationLocation poor = fix(0, 0);
        poor.setAccuracy(80);
        assertFalse(usable(route, Collections.emptyList(), poor));
    }

    private static boolean usable(GeoJsonRoute route, List<LatLon> stops, NavigationLocation latest) {
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1, START, stops,
                END, "trekking", null, Collections.emptyList());
        return BeelineRecoveryRouteValidator.isUsable(route, request, latest);
    }

    private static GeoJsonRoute route(List<LatLon> points, List<VoiceHint> hints) {
        return new GeoJsonRoute(points, hints, 60, 333);
    }

    private static NavigationLocation fix(double lat, double lon) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(lat);
        fix.setLongitude(lon);
        fix.setAccuracy(5);
        return fix;
    }
}
