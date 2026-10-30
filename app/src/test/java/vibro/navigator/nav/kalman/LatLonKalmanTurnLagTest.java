package vibro.navigator.nav.kalman;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class LatLonKalmanTurnLagTest {
    @Test
    public void rightAngleWalkingWithoutTrustedCourseRetainsTheLagBound() {
        for (long interval : new long[] {1_000, 3_000, 5_000, 12_000}) {
            assertTurnLag(interval, false, false);
            assertTurnLag(interval, true, false);
        }
    }

    @Test
    public void reversalRebuildsIndependentEvidenceWithoutWaitingForTheOldLegToExpire() {
        for (long interval : new long[] {1_000, 3_000, 5_000, 12_000}) {
            assertTurnLag(interval, false, true);
            assertTurnLag(interval, true, true);
        }
    }

    private static void assertTurnLag(long interval, boolean speedAccuracy, boolean reverse) {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        for (long time = 0; time <= 240_000; time += interval) {
            NavigationLocation raw = walking(time, reverse);
            if (speedAccuracy) {
                raw.setSpeedAccuracyMetersPerSecond(0.1f);
            }
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            // A reversal needs independent observations along its new leg before recovery.
            if (time >= 30_000 && (!reverse || time <= 60_000 || time >= 90_000)) {
                assertTrue("Turn lag at " + time + " interval=" + interval + " reverse=" + reverse,
                        raw.distanceTo(filtered) <= 3.1);
            }
            assertEquals(30 + raw.distanceTo(filtered), filtered.getAccuracy(), 0.001);
            assertFalse(filtered.hasBearing());
        }
    }

    private static NavigationLocation walking(long time, boolean reverse) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1_700_000_000_000L + time, 1_000 + time);
        double afterTurn = Math.max(0, time / 1_000.0 - 60);
        location.setLatitude((Math.min(time / 1_000.0, 60) - (reverse ? afterTurn : 0)) / 111_320.0);
        location.setLongitude((reverse ? 0 : afterTurn) / 111_320.0);
        location.setAccuracy(30);
        location.setSpeed(1);
        return location;
    }
}
