package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationEtaCadenceTest {
    private final NavigationRouteEtaHistory history = new NavigationRouteEtaHistory();

    @Test
    public void oneSecondWalkingAccumulatesEnoughProgressForLiveTiming() {
        for (int second = 0; second <= 20; second++) {
            RouteMotionEstimate estimate = record(second, second, 12);
            if (second >= 3) {
                assertEquals(1, estimate.speedMps, 0.0001);
            }
            if (second >= 6) {
                assertEquals(0, estimate.accelerationMps2, 0.0001);
            }
        }
    }

    @Test
    public void slowWalkingAtThreeSecondCadenceRetainsLiveSpeed() {
        for (int second = 0; second <= 30; second += 3) {
            RouteMotionEstimate estimate = record(second, second * 0.3, 5);
            if (second >= 6) {
                assertEquals(0.3, estimate.speedMps, 0.0001);
            }
            if (second >= 12) {
                assertTrue(Float.isNaN(estimate.accelerationMps2));
            }
        }
    }

    @Test
    public void aStallOrInsignificantProgressCannotResurrectOlderSpeed() {
        for (int second = 0; second <= 10; second++) {
            record(second, second, 12);
        }
        assertEquals(0, record(13, 10, 12).speedMps, 0);
        assertEquals(0, record(16, 10.1, 12).speedMps, 0);
        assertEquals(0, record(19, 9, 12).speedMps, 0);
    }

    @Test
    public void theMinimumSupportedSpeedRemainsUsable() {
        for (int second = 0; second <= 30; second += 3) {
            RouteMotionEstimate estimate = record(second, second * 0.2, 5);
            if (second >= 9) {
                assertEquals(0.2, estimate.speedMps, 0.0001);
            }
        }
    }

    private RouteMotionEstimate record(int second, double alongMeters, float accuracy) {
        long timeMs = 1_000 + second * 1_000L;
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        return history.record(location, alongMeters, accuracy, true, timeMs);
    }
}
