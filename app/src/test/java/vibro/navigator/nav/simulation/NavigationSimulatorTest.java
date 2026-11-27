package vibro.navigator.nav.simulation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigationSimulatorTest {
    private static final LatLon ORIGIN = new LatLon(0, 0);
    private static final LatLon END = new LatLon(0, 0.002);
    private final List<NavigationLocation> fixes = new ArrayList<>();
    private NavigationSimulationPath path = new NavigationSimulationPath(null, END, null, false);
    private final SimulationTestDriver driver = new SimulationTestDriver(() -> path, fixes::add);

    @Test
    public void projectedStopBarrierHoldsAtTheActualCoordinateUntilDomainReleasesIt() {
        LatLon stop = new LatLon(0.00005, 0.001);
        double along = NavigationSimulationMotion.distance(ORIGIN, new LatLon(0, 0.001));
        path = new NavigationSimulationPath(route(ORIGIN, END), null,
                new NavigationSimulationBarrier(stop, along), false);
        start(50, ORIGIN);
        for (int i = 0; i < 8; i++) { driver.advance(1000); }
        assertEquals(0, distance(stop, last()), 0.01);
        assertEquals(0, last().getSpeed(), 0);
    }

    @Test
    public void restartInsideOutputCannotScheduleDuplicateTicks() {
        SimulationTestDriver[] callbackDriver = new SimulationTestDriver[1];
        callbackDriver[0] = new SimulationTestDriver(() -> path, fix -> {
            fixes.add(fix);
            if (fixes.size() == 1) {
                callbackDriver[0].source.pause();
                callbackDriver[0].source.start();
            }
        });
        callbackDriver[0].source.configure(true, new NavigationSimulationConfig(10, ORIGIN));
        callbackDriver[0].source.start();
        callbackDriver[0].advance(1000);
        callbackDriver[0].advance(1000);
        assertEquals(2, fixes.size());
    }

    @Test
    public void disabledSimulationPassesPhysicalFixesThrough() {
        driver.source.acceptPhysical(realFix(ORIGIN), fixes::add);
        assertEquals("gps", last().getProvider());
    }

    @Test
    public void directMovementHasCorrectSpeedBearingAndSeparateClocks() {
        start(10, ORIGIN);
        driver.advance(1000);
        NavigationLocation fix = last();
        assertEquals(10, distance(ORIGIN, fix), 0.01);
        assertEquals(10, fix.getSpeed(), 0.01);
        assertEquals(90, fix.getBearing(), 0.01);
        assertEquals(driver.nowMs(), fix.getElapsedRealtimeMs());
        assertTrue(fix.getTime() > fix.getElapsedRealtimeMs());
        assertEquals(NavigationSimulator.PROVIDER, fix.getProvider());
    }

    @Test
    public void directSimulationCrossesAntimeridianAtConfiguredSpeed() {
        LatLon origin = new LatLon(0, 179.999);
        LatLon target = new LatLon(0, -179.999);
        path = new NavigationSimulationPath(null, target, null, false);
        start(50f / 3.6f, origin);
        driver.advance(1000);
        assertEquals(50.0 / 3.6, distance(origin, last()), 0.01);
        for (int second = 1; second < 9; second++) { driver.advance(1000); }
        assertTrue(last().getLongitude() < -179.999);
        assertEquals(50.0 / 3.6, last().getSpeed(), 0.01);
        for (int second = 9; second < 19; second++) { driver.advance(1000); }
        assertEquals(target.lon, last().getLongitude(), 0);
        assertEquals(0, last().getSpeed(), 0);
    }

    @Test
    public void routedSimulationCrossesAntimeridianFromAnInteriorStart() {
        LatLon origin = new LatLon(0, -179.9998);
        LatLon target = new LatLon(0, -179.999);
        path = new NavigationSimulationPath(route(new LatLon(0, 179.999), target), null, null, false);
        start(10, origin);
        driver.advance(1000);
        assertEquals(10, distance(origin, last()), 0.03);
        assertTrue(last().getLongitude() > origin.lon);
        assertEquals(10, last().getSpeed(), 0.03);
    }

    @Test
    public void blankOriginWaitsForPhysicalSeedAndIgnoresLaterPhysicalMovement() {
        start(10, null);
        driver.advance(1000);
        assertTrue(fixes.isEmpty());
        assertNull(driver.source.currentFix());
        NavigationLocation coarse = realFix(END);
        coarse.setAccuracy(600);
        assertFalse(driver.source.seed(coarse));
        NavigationLocation stale = realFix(END);
        stale.setTime(System.currentTimeMillis(), driver.nowMs() - 30_000);
        assertFalse(driver.source.seed(stale));
        NavigationLocation real = realFix(ORIGIN);
        assertTrue(driver.source.acceptPhysical(real, ignored -> { throw new AssertionError(); }));
        assertFalse(driver.source.acceptPhysical(realFix(END), ignored -> { throw new AssertionError(); }));
        driver.advance(1000);
        assertEquals(10, distance(ORIGIN, last()), 0.01);
    }

    @Test
    public void pauseResumeDoesNotAccumulatePausedTimeAndStopCancelsTicks() {
        start(10, ORIGIN);
        driver.advance(1000);
        driver.source.pause();
        driver.advance(60000);
        assertEquals(1, fixes.size());
        driver.source.start();
        driver.advance(1000);
        assertEquals(20, distance(ORIGIN, last()), 0.01);
        driver.source.pause();
        driver.advance(1000);
        assertEquals(2, fixes.size());
    }

    @Test
    public void delayedTickCannotTeleportAndZeroSpeedHolds() {
        start(10, ORIGIN);
        driver.advance(60000);
        assertEquals(20, distance(ORIGIN, last()), 0.01);
        driver.source.configure(true, new NavigationSimulationConfig(0, ORIGIN));
        driver.source.start();
        driver.advance(1000);
        assertEquals(0, distance(ORIGIN, last()), 0);
        assertEquals(0, last().getSpeed(), 0);
    }

    @Test
    public void waitsWhileLoadingThenFollowsCornersAndStopsAtDomainBarrier() {
        path = new NavigationSimulationPath(null, null, null, false);
        start(20, ORIGIN);
        driver.advance(1000);
        assertEquals(0, distance(ORIGIN, last()), 0);
        GeoJsonRoute route = route(ORIGIN, new LatLon(0, 0.0001), new LatLon(0.0001, 0.0001));
        path = new NavigationSimulationPath(route, null, new NavigationSimulationBarrier(
                route.track.get(1), new vibro.navigator.nav.route.PolylineIndex(route.track).distanceAtPointIndex(1)), false);
        driver.advance(1000);
        assertEquals(0, last().getLatitude(), 0);
        assertEquals(0.0001, last().getLongitude(), 0.0000001);
        driver.advance(1000);
        assertEquals(0, last().getSpeed(), 0);
        path = new NavigationSimulationPath(route, null, null, false);
        driver.advance(1000);
        assertEquals(0.0001, last().getLatitude(), 0.0000001);
        driver.advance(1000);
        assertEquals(0, last().getSpeed(), 0);
    }

    @Test
    public void closedRouteKeepsCursorThroughReturnToOrigin() {
        GeoJsonRoute route = route(ORIGIN, new LatLon(0, 0.0001), new LatLon(0.0001, 0.0001), ORIGIN);
        path = new NavigationSimulationPath(route, null, null, false);
        start(10, ORIGIN);
        for (int i = 0; i < 8; i++) { driver.advance(1000); }
        assertEquals(0, distance(ORIGIN, last()), 0.01);
        assertEquals(0, last().getSpeed(), 0);
    }

    @Test
    public void replacedRouteReanchorsWithoutJumpingOrRetainingOldGeometry() {
        start(10, ORIGIN);
        driver.advance(1000);
        LatLon before = new LatLon(last().getLatitude(), last().getLongitude());
        path = new NavigationSimulationPath(route(before, new LatLon(0.001, before.lon)), null, null, false);
        driver.advance(1000);
        assertEquals(10, distance(before, last()), 0.01);
        assertEquals(before.lon, last().getLongitude(), 0.0000001);
        assertTrue(last().getLatitude() > 0);
    }

    @Test
    public void confirmedArrivalHoldsEvenIfTargetStillPresent() {
        path = new NavigationSimulationPath(null, END, null, true);
        start(10, ORIGIN);
        driver.advance(1000);
        assertEquals(0, distance(ORIGIN, last()), 0);
        assertNotNull(driver.source.currentFix());
    }

    private void start(float speed, LatLon origin) {
        driver.source.configure(true, new NavigationSimulationConfig(speed, origin));
        driver.source.start();
    }

    private NavigationLocation last() { return fixes.get(fixes.size() - 1); }

    private static double distance(LatLon from, NavigationLocation location) {
        return NavigationSimulationMotion.distance(from, new LatLon(location.getLatitude(), location.getLongitude()));
    }

    private NavigationLocation realFix(LatLon point) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(System.currentTimeMillis(), driver.nowMs());
        location.setAccuracy(5);
        location.setLatitude(point.lat);
        location.setLongitude(point.lon);
        return location;
    }

    private static GeoJsonRoute route(LatLon... points) {
        return new GeoJsonRoute(Arrays.asList(points), Collections.emptyList(), 0, 0);
    }
}
