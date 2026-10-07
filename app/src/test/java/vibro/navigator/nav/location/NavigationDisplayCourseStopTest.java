package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationDisplayCourseStopTest {
    @Test
    public void stopSeparatesIncomingHistoryFromReversedDisplayCourse() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        record(model, 1_000L, 0, 1.4f);
        record(model, 4_000L, 10, 1.4f);
        NavigationLocation incoming = record(model, 7_000L, 20, 1.4f);
        assertEquals(0.0, model.displayMovementCourse(incoming).headingDegrees, 0.01);
        record(model, 10_000L, 30, 1.4f);
        record(model, 13_000L, 30, 0f);
        assertTrue(model.isLikelyStationary());
        NavigationLocation firstReturn = record(model, 16_000L, 28, 1.4f);
        assertNull(model.displayMovementCourse(firstReturn));
        NavigationLocation returnFix = record(model, 19_000L, 25, 1.4f);
        NavigationLocationMotionModel.Course course = model.displayMovementCourse(returnFix);
        assertNotNull(course);
        assertEquals(180.0, course.headingDegrees, 0.01);
    }

    private static NavigationLocation record(NavigationLocationMotionModel model, long timeMs,
            double northMeters, float speed) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setTime(timeMs, timeMs);
        location.setLatitude(northMeters / 111_320.0);
        location.setLongitude(0);
        location.setAccuracy(1f);
        location.setSpeed(speed);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        model.recordFilteredLocation(location);
        return location;
    }
}
