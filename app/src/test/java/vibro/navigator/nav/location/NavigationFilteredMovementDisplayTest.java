package vibro.navigator.nav.location;

import org.junit.Test;
import vibro.navigator.nav.kalman.LatLonKalmanFilter;
import static org.junit.Assert.*;

public class NavigationFilteredMovementDisplayTest {
    @Test
    public void walkingRetainsDisplaySpeedWithConservativeFilteredUncertainty() {
        for (float accuracy : new float[] {5, 13, 30}) {
            assertWalkingDisplay(accuracy, false, false);
            assertWalkingDisplay(accuracy, true, false);
        }
    }

    @Test
    public void aTurnRetainsDisplaySpeedWhileTheOldLegStopsCorroboratingMovement() {
        assertWalkingDisplay(13, false, true);
        assertWalkingDisplay(13, true, true);
    }

    @Test
    public void confirmedSpeedlessSlowWalkingRetainsFilteredDisplaySpeed() {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i < 30; i++) {
            NavigationLocation raw = new NavigationLocation("gps");
            raw.setTime(1_000 + i * 3_000L, 1_000 + i * 3_000L);
            raw.setLatitude(i * 0.9 / 111_320);
            raw.setAccuracy(0.5f);
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            model.recordLocation(raw, filtered);
            if (i >= 15) {
                assertEquals(NavigationMotionState.MOVING, model.motionState());
                assertEquals(0.3f, model.displaySpeedMps(filtered), 0.01f);
            }
        }
    }

    private static void assertWalkingDisplay(float accuracy, boolean speedQuality, boolean turn) {
        LatLonKalmanFilter filter = new LatLonKalmanFilter();
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        for (int i = 0; i < 80; i++) {
            NavigationLocation raw = walking(i, accuracy, turn);
            if (speedQuality) {
                raw.setSpeedAccuracyMetersPerSecond(0.1f);
            }
            NavigationLocation filtered = filter.update(raw);
            assertNotNull(filtered);
            model.recordLocation(raw, filtered);
            if (i >= 15) {
                assertEquals(NavigationMotionState.MOVING, model.motionState());
                assertEquals("Display speed at " + i + " accuracy=" + accuracy, 1, model.displaySpeedMps(filtered), 0);
            }
        }
    }

    private static NavigationLocation walking(int index, float accuracy, boolean turn) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(1_000 + index * 3_000L, 1_000 + index * 3_000L);
        double distance = index * 3;
        location.setLatitude((turn ? Math.min(distance, 60) : distance) / 111_320.0);
        location.setLongitude((turn ? Math.max(0, distance - 60) : 0) / 111_320.0);
        location.setAccuracy(accuracy);
        location.setSpeed(1);
        return location;
    }
}
