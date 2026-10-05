package vibro.navigator.nav.location;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocationMotionModel.Course;

/** Display-only course history, independent of the motion and reroute evidence windows. */
final class NavigationDisplayCourseHistory {
    // Preserve a fix across the longest 60-second dynamic interval plus acquisition slack.
    private static final long HISTORY_WINDOW_MS = 90_000L;
    private static final long MIN_COURSE_SPAN_MS = 2_000L;
    private static final long MAX_CURRENT_COURSE_SPAN_MS = 15_000L;
    private static final double MIN_COURSE_DISTANCE_METERS = 3.0;
    private static final float MAX_CURRENT_COURSE_ACCURACY_DEGREES = 25f;
    private static final float HISTORICAL_COURSE_UNCERTAINTY_DEGREES = 90f;

    private final ArrayDeque<NavigationLocation> locations = new ArrayDeque<>();

    void reset() {
        locations.clear();
    }

    void record(NavigationLocation location) {
        locations.addLast(new NavigationLocation(location));
        prune(location.getElapsedRealtimeOrTimeMs());
    }

    @Nullable
    Course estimate(NavigationLocation location) {
        prune(location.getElapsedRealtimeOrTimeMs());
        Iterator<NavigationLocation> samples = locations.descendingIterator();
        Course uncertainCourse = null;
        while (samples.hasNext()) {
            Course course = courseBetween(samples.next(), location);
            if (course == null) {
                continue;
            }
            if (course.accuracyDegrees <= MAX_CURRENT_COURSE_ACCURACY_DEGREES) {
                return course;
            }
            if (uncertainCourse == null) {
                uncertainCourse = course;
            }
        }
        return uncertainCourse;
    }

    @Nullable
    private static Course courseBetween(NavigationLocation earlier, NavigationLocation current) {
        long elapsedMs = current.getElapsedRealtimeOrTimeMs() - earlier.getElapsedRealtimeOrTimeMs();
        if (elapsedMs < MIN_COURSE_SPAN_MS || !hasUsableAccuracy(earlier) || !hasUsableAccuracy(current)) {
            return null;
        }
        double distance = GeoMath.distanceMeters(earlier.getLatitude(), earlier.getLongitude(),
                current.getLatitude(), current.getLongitude());
        double positionUncertainty = (double) earlier.getAccuracy() + current.getAccuracy();
        if (!hasUsableDisplacement(distance, positionUncertainty)) {
            return null;
        }
        float accuracyDegrees = elapsedMs > MAX_CURRENT_COURSE_SPAN_MS
                ? HISTORICAL_COURSE_UNCERTAINTY_DEGREES
                : (float) Math.toDegrees(Math.asin(positionUncertainty / distance));
        return new Course(GeoMath.bearingDegrees(earlier.getLatitude(), earlier.getLongitude(),
                current.getLatitude(), current.getLongitude()), accuracyDegrees);
    }

    private static boolean hasUsableAccuracy(NavigationLocation location) {
        return location.hasAccuracy() && Float.isFinite(location.getAccuracy()) && location.getAccuracy() >= 0f;
    }

    private static boolean hasUsableDisplacement(double distance, double positionUncertainty) {
        return Double.isFinite(distance) && distance >= MIN_COURSE_DISTANCE_METERS && distance > positionUncertainty;
    }

    private void prune(long newestTimeMs) {
        long cutoffTimeMs = newestTimeMs - HISTORY_WINDOW_MS;
        while (!locations.isEmpty() && locations.peekFirst().getElapsedRealtimeOrTimeMs() < cutoffTimeMs) {
            locations.removeFirst();
        }
    }
}
