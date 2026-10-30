package vibro.navigator.nav.route;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationRoundTripDepartureQualityTest {
    private static final LatLon RETURN = new LatLon(.000005, .000005);
    private final NavigationRouteGeometryState state = new NavigationRouteGeometryState();

    @Test
    public void poorRawFixCannotUnlockArrivalAtTheLoopStart() {
        checkUnusableDeparture(75, 5);
    }

    @Test
    public void poorFilteredFixCannotUnlockArrivalAtTheLoopStart() {
        checkUnusableDeparture(5, 75);
    }

    @Test
    public void poorRawAndFilteredFixCannotUnlockArrivalAtTheLoopStart() {
        checkUnusableDeparture(75, 75);
    }

    @Test
    public void missingInvalidAndJustOverBoundaryAccuracyCannotUnlockArrival() {
        for (float accuracy : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -1, 25.01f}) {
            checkUnusableDeparture(accuracy, 5);
            checkUnusableDeparture(5, accuracy);
        }
        load();
        NavigationLocation missingAccuracy = new NavigationLocation("gps");
        missingAccuracy.setTime(1_000, 1_000);
        missingAccuracy.setLongitude(.0012);
        assertFalse(state.isWithinDestinationReachedRadius(missingAccuracy, 5, state.match(missingAccuracy, 5), 1_000));
        assertFalse(observe(RETURN, 4_000, 5, 5));
        assertFalse(observe(RETURN, 7_000, 5, 5));
    }

    @Test
    public void clearingArrivalEvidencePreservesAUsableDeparture() {
        load();
        assertFalse(observe(new LatLon(0, .0012), 1_000, 25, 25));
        assertFalse(observe(RETURN, 4_000, 5, 5));
        state.clearArrivalEvidence();
        assertFalse(observe(RETURN, 7_000, 5, 5));
        assertTrue(observe(RETURN, 10_000, 5, 5));
    }

    private void checkUnusableDeparture(float rawAccuracy, float trustedAccuracy) {
        load();
        assertFalse(observe(new LatLon(0, .0012), 1_000, rawAccuracy, trustedAccuracy));
        assertFalse(observe(RETURN, 4_000, 5, 5));
        assertFalse("Poor departure evidence must not enable a finish at the start", observe(RETURN, 7_000, 5, 5));
        assertFalse(observe(new LatLon(0, .0012), 10_000, 5, 5));
        assertFalse(observe(RETURN, 13_000, 5, 5));
        assertTrue("Usable departure still permits a confirmed finish", observe(RETURN, 16_000, 5, 5));
    }

    private void load() {
        state.loadRoute(new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, .003),
                new LatLon(.003, .003), new LatLon(0, 0)), Collections.emptyList(), 1_000, 1_140), true);
    }

    private boolean observe(LatLon point, long time, float rawAccuracy, float trustedAccuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(time, time);
        location.setLatitude(point.lat);
        location.setLongitude(point.lon);
        location.setAccuracy(rawAccuracy);
        return state.isWithinDestinationReachedRadius(location, trustedAccuracy,
                state.match(location, trustedAccuracy), time);
    }
}
