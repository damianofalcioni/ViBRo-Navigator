package vibro.navigator.nav.kalman;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class LatLonKalmanJumpTest {
    private static final double LON = 16.37;
    private static final double EAST_SCALE = 111320 * Math.cos(Math.toRadians(48.2));

    @Test
    public void betterAccuracyAtSameTimeCannotBypassJumpCheck() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 5, 0));
        assertNull(filter.update(fix(1000, 100, 1, 0)));
        assertEquals(0, east(filter.update(fix(1000, 0, 1, 0))), 0.001);
    }

    @Test
    public void missingSpeedUsesTravelAllowanceAndRejectsImpossibleJump() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(positionOnlyFix(1000, 0, 5));
        assertNull(filter.update(positionOnlyFix(2000, 1000, 5)));
        assertNotNull(filter.update(positionOnlyFix(3000, 1, 5)));
        assertNotNull(filter.update(positionOnlyFix(63000, 1000, 5)));
    }

    @Test
    public void outlierAfterSparseTravelDoesNotPoisonFasterFixes() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 5, 1.4f));
        filter.update(fix(61000, 84, 5, 1.4f));
        assertNull(filter.update(fix(121000, 5000, 5, 0)));
        NavigationLocation out = filter.update(fix(124000, 168, 5, 0));
        assertNotNull(out);
        assertEquals(168, east(out), 0.001);
    }

    @Test
    public void sparseTravelAllowanceDoesNotCapCredibleHigherSpeed() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        filter.update(fix(1000, 0, 5, 70));
        NavigationLocation out = filter.update(fix(61000, 4200, 5, 70));
        assertNotNull(out);
        assertEquals(4200, east(out), 0.001);
    }

    private static NavigationLocation fix(long time, double east, float acc, float speed) {
        NavigationLocation location = positionOnlyFix(time, east, acc);
        location.setSpeed(speed);
        return location;
    }

    private static NavigationLocation positionOnlyFix(long time, double east, float acc) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1700000000000L + time, time);
        location.setLatitude(48.2);
        location.setLongitude(LON + east / EAST_SCALE);
        location.setAccuracy(acc);
        return location;
    }

    private static double east(NavigationLocation location) {
        return (location.getLongitude() - LON) * EAST_SCALE;
    }
}
