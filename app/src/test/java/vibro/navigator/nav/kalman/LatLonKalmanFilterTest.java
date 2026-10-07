package vibro.navigator.nav.kalman;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

import static org.junit.Assert.*;

public class LatLonKalmanFilterTest {
    private static final double LAT = 48.2;
    private static final double LON = 16.37;
    private static final double EAST_SCALE = 111320 * Math.cos(Math.toRadians(LAT));

    @Test
    public void betterStationaryFixCorrectsStartupBiasWithoutOvershoot() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 23, 0, 25, 0));
        for (long time = 2000; time <= 12000; time += 1000) {
            NavigationLocation out = filter.update(fix(time, 3, 0, 5, 0));
            assertNotNull(out);
            assertEquals(3, east(out), 0.001);
            assertEquals(5, out.getAccuracy(), 0.001);
        }
    }

    @Test
    public void stationaryCorrectionsDoNotCreateMomentumAcrossPath() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 20, 0, 5, 0));
        for (long time = 2000; time <= 15000; time += 1000) {
            NavigationLocation out = filter.update(fix(time, 3, 0, 5, 0));
            assertNotNull(out);
            assertTrue(east(out) >= 3 - 0.001);
            assertTrue(east(out) <= 8.001);
        }
    }

    @Test
    public void smoothingRadiusIncludesShiftFromIncomingFix() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 0, 5, 0));
        NavigationLocation raw = fix(2000, 4, 0, 5, 0);
        NavigationLocation out = filter.update(raw);
        assertNotNull(out);
        assertTrue(raw.distanceTo(out) > 1);
        assertEquals(5 + raw.distanceTo(out), out.getAccuracy(), 0.001);
        assertEquals(5, raw.getAccuracy(), 0);
        assertEquals(5, new NavigationLocation(out).getProviderAccuracy(), 0);
    }

    @Test
    public void isolatedOutlierDoesNotDisplaceFilterOrPoisonNextFix() {
        for (long interval : new long[]{1000, 3000, 60000}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            filter.update(fix(1000, 0, 0, 5, 0));
            double jump = interval < 15000 ? 40 : 5000;
            assertNull(filter.update(fix(1000 + interval, jump, 0, 5, 0)));
            NavigationLocation good = filter.update(fix(1000 + 2 * interval, 0, 0, 5, 0));
            assertNotNull(good);
            assertEquals(0, east(good), 0.001);
        }
    }

    @Test
    public void repeatedCoherentJumpRecoversRealRelocation() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 0, 5, 0));
        assertNull(filter.update(fix(2000, 100, 0, 5, 0)));
        NavigationLocation confirmed = filter.update(fix(3000, 101, 0, 5, 0));
        assertNotNull(confirmed);
        assertEquals(101, east(confirmed), 0.001);
    }

    @Test
    public void outOfOrderAndDuplicateFixesCannotRewindOrReweightState() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 0, 5, 0));
        filter.update(fix(3000, 2, 0, 5, 0));
        assertNull(filter.update(fix(2500, 50, 0, 1, 0)));
        assertNull(filter.update(fix(3000, 50, 0, 5, 0)));
        NavigationLocation good = filter.update(fix(4000, 2, 0, 5, 0));
        assertNotNull(good);
        assertTrue(east(good) >= 0 && east(good) <= 2.001);
    }

    @Test
    public void improvedSameTimeFixReplacesObservation() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 20, 0, 25, 0));
        NavigationLocation better = filter.update(fix(1000, 0, 0, 5, 0));
        assertNotNull(better);
        assertEquals(0, east(better), 0.001);
    }

    @Test
    public void malformedAccuracyCannotPoisonFilter() {
        for (float accuracy : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -1}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            assertNull(filter.update(fix(1000, 0, 0, accuracy, 0)));
            assertNotNull(filter.update(fix(2000, 0, 0, 5, 0)));
            assertNull(filter.update(fix(3000, 0, 0, accuracy, 0)));
            NavigationLocation good = filter.update(fix(4000, 0, 0, 5, 0));
            assertNotNull(good);
            assertTrue(Double.isFinite(good.getLongitude()));
            assertEquals(0, east(good), 0.001);
        }
    }

    @Test
    public void malformedCoordinatesCannotPoisonFilter() {
        for (double latitude : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 91, -91}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            NavigationLocation invalid = fix(1000, 0, 0, 5, 0);
            invalid.setLatitude(latitude);
            assertNull(filter.update(invalid));
            assertNotNull(filter.update(fix(2000, 0, 0, 5, 0)));
        }
        for (double longitude : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 181, -181}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            NavigationLocation invalid = fix(1000, 0, 0, 5, 0);
            invalid.setLongitude(longitude);
            assertNull(filter.update(invalid));
            assertNotNull(filter.update(fix(2000, 0, 0, 5, 0)));
        }
    }

    @Test
    public void datelineCrossingUsesShortLongitudeDifferenceBothDirections() {
        for (double direction : new double[]{-1, 1}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            NavigationLocation start = fix(1000, 0, 0, 5, 1);
            start.setLatitude(0);
            start.setLongitude(direction * 179.99999);
            filter.update(start);
            NavigationLocation next = new NavigationLocation(start);
            next.setTime(2000, 2000);
            next.setLongitude(-start.getLongitude());
            NavigationLocation out = filter.update(next);
            assertNotNull(out);
            assertTrue(Math.abs(out.getLongitude()) > 179.9999);
            assertTrue(out.distanceTo(next) < 3);
        }
    }

    @Test
    public void polesAndZeroAccuracyRemainFinite() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocation first = fix(1000, 0, 0, 0, 0);
        first.setLatitude(90);
        filter.update(first);
        NavigationLocation next = new NavigationLocation(first);
        next.setTime(2000, 2000);
        NavigationLocation out = filter.update(next);
        assertNotNull(out);
        assertTrue(Double.isFinite(out.getLongitude()));
        assertTrue(Float.isFinite(out.getAccuracy()));
    }

    @Test
    public void missingAccuracyStaysUnknown() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocation first = new NavigationLocation("gps");
        first.setLatitude(LAT);
        first.setLongitude(LON);
        first.setTime(1000, 1000);
        filter.update(first);
        first.setTime(2000, 2000);
        assertFalse(filter.update(first).hasAccuracy());
    }

    @Test
    public void stopsDoNotOvershootAtWalkingOrDrivingSpeeds() {
        for (float speed : new float[]{1.4f, 15f}) {
            for (long interval : new long[]{1000, 3000}) {
                LatLonKalmanFilter filter = new LatLonKalmanFilter();
                for (long time = 1000; time <= 61000; time += interval) {
                    filter.update(courseFix(time, (time - 1000) * speed / 1000, 0, speed, 90));
                }
                double stop = 60 * speed;
                for (long time = 61000 + interval; time <= 76000; time += interval) {
                    NavigationLocation out = filter.update(fix(time, stop, 0, 5, 0));
                    assertNotNull(out);
                    assertTrue("stop overshoot", east(out) <= stop + 0.01);
                    assertTrue("stop lag", east(out) >= stop - 5.01);
                }
            }
        }
    }

    @Test
    public void trustedTurnClearsPreviousCourse() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(courseFix(1000, 0, 0, 15, 90));
        filter.update(courseFix(2000, 15, 0, 15, 90));
        NavigationLocation turn = courseFix(3000, 15, 15, 15, 0);
        assertEquals(0, filter.update(turn).distanceTo(turn), 0.001);
    }

    @Test
    public void untrustedCourseCannotPredictAcrossPath() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocation first = fix(1000, 0, 0, 5, 1.4f);
        first.setBearing(270);
        filter.update(first);
        NavigationLocation next = fix(2000, 1.4, 0, 5, 1.4f);
        next.setBearing(270);
        NavigationLocation out = filter.update(next);
        assertTrue(east(out) >= -0.001);
        assertTrue(east(out) <= 1.401);
    }

    @Test
    public void sparseStopsAndReturnToFastFixesDoNotInventReverseMotion() {
        for (double position : new double[]{0, 84}) {
            LatLonKalmanFilter filter = new LatLonKalmanFilter();
            filter.update(courseFix(1000, 0, 0, 1.4f, 90));
            filter.update(courseFix(61000, 84, 0, 1.4f, 90));
            filter.update(fix(121000, position, 0, 5, 0));
            for (long time = 124000; time <= 136000; time += 3000) {
                NavigationLocation out = filter.update(fix(time, position, 0, 5, 0));
                assertNotNull(out);
                assertTrue(east(out) >= position - 0.001);
                assertTrue(east(out) <= position + 5.001);
            }
        }
    }

    @Test
    public void sparseStationaryFixesStillSmoothJitter() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 5, 0, 5, 0));
        NavigationLocation next = filter.update(fix(61000, -5, 0, 5, 0));
        assertNotNull(next);
        assertTrue(Math.abs(east(next)) < 1);
    }

    @Test
    public void zeroSpeedEndpointsDoNotExcludeMovementBetweenSparseFixes() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 0, 5, 0));
        NavigationLocation next = filter.update(fix(61000, 84, 0, 5, 0));
        assertNotNull(next);
        assertEquals(84, east(next), 0.001);
        assertEquals(84, east(filter.update(fix(64000, 84, 0, 5, 0))), 0.001);
    }

    @Test
    public void sixtySecondTravelAndFasterFixesKeepNewCourse() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(courseFix(1000, 0, 0, 1.4f, 90));
        filter.update(courseFix(61000, 84, 0, 1.4f, 90));
        NavigationLocation turned = courseFix(121000, 84, 84, 1.4f, 0);
        assertEquals(0, filter.update(turned).distanceTo(turned), 0.001);
        NavigationLocation next = courseFix(124000, 84, 88.2, 1.4f, 0);
        NavigationLocation out = filter.update(next);
        assertEquals(84, east(out), 0.001);
        assertEquals(88.2, north(out), 0.001);
    }

    private static NavigationLocation fix(long time, double east, double north, float acc, float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1700000000000L + time, time);
        location.setLatitude(LAT + north / 111320);
        location.setLongitude(LON + east / EAST_SCALE);
        location.setAccuracy(acc);
        location.setSpeed(speed);
        return location;
    }

    private static NavigationLocation courseFix(long time, double east, double north, float speed, float bearing) {
        NavigationLocation location = fix(time, east, north, 5, speed);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        location.setBearing(bearing);
        location.setBearingAccuracyDegrees(5);
        return location;
    }

    private static double east(NavigationLocation location) {
        return (location.getLongitude() - LON) * EAST_SCALE;
    }

    private static double north(NavigationLocation location) {
        return (location.getLatitude() - LAT) * 111320;
    }
}
