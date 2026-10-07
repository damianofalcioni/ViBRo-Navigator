package vibro.navigator.nav.session;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NavigationSessionRouteHeadingFallbackTest {
    private final NavigationSessionLocationState locations = new NavigationSessionLocationState();
    private final NavigationSessionHeadingResolver resolver = new NavigationSessionHeadingResolver(locations);

    @Test
    public void reverseWalkingHeadingRequiresIndependentFixesAndPreservesAccuracy() {
        NavigationLocation first = fix(1_000, 0, 180f, 12f);
        assertRoute(first);
        assertEquals(0.0, select(first, 2_000).headingDegrees, 0.0);
        assertRoute(fix(1_500, -1, 180f, 12f));
        NavigationSessionHeadingResolver.Selection heading = select(fix(2_000, -2, 180f, 12f), 2_000);
        assertEquals(180.0, heading.headingDegrees, 0.0);
        assertEquals(12f, heading.headingAccuracyDegrees, 0f);
    }

    @Test
    public void poorWalkingBearingNeedsDisplacementBeyondPositionUncertainty() {
        assertRoute(fix(1_000, 0, 180f, 60f));
        assertRoute(fix(3_000, -2, 180f, 60f));
        NavigationSessionHeadingResolver.Selection heading = select(fix(9_000, -12, 180f, 60f), 9_000);
        assertEquals(180.0, heading.headingDegrees, 0.0);
        assertEquals(60f, heading.headingAccuracyDegrees, 0f);
    }

    @Test
    public void repeatedPoorBearingOppositeToActualDisplacementKeepsRoute() {
        assertRoute(fix(1_000, 0, 180f, 60f));
        assertRoute(fix(9_000, 12, 180f, 60f));
    }

    @Test
    public void moderateDisagreementAndExcessiveUncertaintyKeepRoute() {
        assertRoute(fix(1_000, 0, 120f, 12f));
        assertRoute(fix(3_000, 0, 120f, 12f));
        assertRoute(fix(5_000, 0, 150f, 70f));
        assertRoute(fix(7_000, -12, 150f, 70f));
        assertRoute(fix(9_000, -15, 180f, 100f));
    }

    @Test
    public void inconsistentBearingsRestartConfirmation() {
        assertRoute(fix(1_000, 0, 135f, 12f));
        assertRoute(fix(3_000, -2, 180f, 12f));
        assertEquals(180.0, select(fix(5_000, -4, 180f, 12f), 5_000).headingDegrees, 0.0);
    }

    @Test
    public void recoveryRequiresRepeatedAgreementAndKeepsHysteresis() {
        activate();
        assertEquals(80.0, select(fix(5_000, -3, 80f, 12f), 5_000).headingDegrees, 0.0);
        assertEquals(20.0, select(fix(7_000, -2, 20f, 12f), 7_000).headingDegrees, 0.0);
        assertRoute(fix(9_000, 0, 20f, 12f));
    }

    @Test
    public void staleFixDoesNotDisplayLocationHeadingAndSparseFreshFixCanConfirm() {
        NavigationLocation first = fix(1_000, 0, 180f, 12f);
        assertRoute(first);
        assertEquals(0.0, select(first, 60_000).headingDegrees, 0.0);
        assertEquals(180.0, select(fix(61_000, -20, 180f, 12f), 61_000).headingDegrees, 0.0);
        assertEquals(0.0, select(fix(61_000, -20, 180f, 12f), 72_000).headingDegrees, 0.0);
    }

    @Test
    public void longGapAndSessionResetRequireNewConfirmation() {
        activate();
        assertRoute(fix(100_000, -20, 180f, 12f));
        resolver.reset();
        assertRoute(fix(102_000, -22, 180f, 12f));
    }

    @Test
    public void stationarityAndBeelinesClearRouteDisagreement() {
        activate();
        NavigationLocation stop = fix(5_000, -2, 180f, 12f);
        assertEquals(180.0, resolver.selectHeading(stop, true, 220.0, 5f,
                5_000, 0.0, false).headingDegrees, 0.0);
        assertRoute(fix(7_000, -3, 180f, 12f));
        NavigationLocation beeline = fix(9_000, -4, 180f, 12f);
        assertEquals(180.0, resolver.selectHeading(beeline, false, 220.0, 5f,
                9_000, null, true).headingDegrees, 0.0);
        assertRoute(fix(11_000, -5, 180f, 12f));
    }

    @Test
    public void routeReplacementAndMotionResetClearDisagreement() {
        resolver.synchronizeRoute(route());
        activate();
        resolver.synchronizeRoute(route());
        assertRoute(fix(5_000, -4, 180f, 12f));
        assertEquals(180.0, select(fix(7_000, -5, 180f, 12f), 7_000).headingDegrees, 0.0);
        locations.reset();
        assertRoute(fix(9_000, -6, 180f, 12f));
    }

    @Test
    public void poorBearingFallbackDoesNotBecomeTrustedRerouteBearing() {
        NavigationLocation poor = fix(1_000, 0, 180f, 60f);
        assertNull(locations.trustedActualBearingDegreesForReroute(poor));
        assertRoute(poor);
        assertEquals(180.0, select(fix(9_000, -12, 180f, 60f), 9_000).headingDegrees, 0.0);
        assertNull(locations.trustedActualBearingDegreesForReroute(poor));
    }

    @Test
    public void invalidAccuracyAndSlowSpeedCannotEnableFallback() {
        NavigationLocation bad = fix(1_000, 0, 180f, Float.NaN);
        assertRoute(bad);
        assertRoute(fix(3_000, -12, 180f, -1f));
        NavigationLocation slow = fix(5_000, -24, 180f, 12f);
        slow.setSpeed(0.1f);
        assertRoute(slow);
        NavigationLocation unknown = new NavigationLocation("gps");
        unknown.setTime(7_000, 7_000);
        unknown.setSpeed(15f);
        unknown.setBearing(180f);
        assertRoute(unknown);
    }

    @Test
    public void angularWrapDoesNotInventRouteDisagreement() {
        NavigationLocation location = fix(1_000, 0, 5f, 12f);
        assertEquals(355.0, resolver.selectHeading(location, false, 220.0, 5f,
                1_000, 355.0, false).headingDegrees, 0.0);
        location = fix(3_000, 2, 5f, 12f);
        assertEquals(355.0, resolver.selectHeading(location, false, 220.0, 5f,
                3_000, 355.0, false).headingDegrees, 0.0);
    }

    @Test
    public void unusableHeadingClearsActiveDisagreement() {
        activate();
        assertRoute(fix(5_000, -4, 180f, Float.NaN));
        assertRoute(fix(7_000, -6, 180f, 12f));
    }

    private void activate() {
        assertRoute(fix(1_000, 0, 180f, 12f));
        assertEquals(180.0, select(fix(3_000, -2, 180f, 12f), 3_000).headingDegrees, 0.0);
    }

    private void assertRoute(NavigationLocation location) {
        NavigationSessionHeadingResolver.Selection heading = select(location, location.getElapsedRealtimeOrTimeMs());
        assertEquals(0.0, heading.headingDegrees, 0.0);
        assertEquals(0f, heading.headingAccuracyDegrees, 0f);
    }

    private NavigationSessionHeadingResolver.Selection select(NavigationLocation location, long nowMs) {
        return resolver.selectHeading(location, false, 220.0, 5f, nowMs, 0.0, false);
    }

    private static NavigationLocation fix(long time, double northMeters, float bearing, float bearingAccuracy) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(time, time);
        fix.setLatitude(northMeters / 111_195.0);
        fix.setAccuracy(5f);
        fix.setSpeed(1.2f);
        fix.setBearing(bearing);
        fix.setBearingAccuracyDegrees(bearingAccuracy);
        return fix;
    }

    private static GeoJsonRoute route() {
        return new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0.001, 0)),
                Collections.emptyList(), 100, 111);
    }
}
