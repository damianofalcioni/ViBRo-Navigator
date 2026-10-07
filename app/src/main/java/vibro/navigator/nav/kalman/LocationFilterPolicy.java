package vibro.navigator.nav.kalman;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.nav.location.NavigationLocation;

/** Measurement quality and cadence policy, independent of position-filter state. */
final class LocationFilterPolicy {
    // Android accuracy is a radial 68% radius, not a one-axis standard deviation.
    private static final double RADIAL_VARIANCE_FACTOR = -2 * Math.log(0.32);
    private static final double DEFAULT_ACCURACY_METERS = 20;
    private static final double STATIONARY_SPEED_MPS = 0.35;
    // Course at a fix cannot describe motion across a sparse interval. Keep stationary
    // position smoothing, but re-anchor moving/stopping fixes rather than infer a velocity.
    private static final double SPARSE_INTERVAL_SECONDS = 15;

    private LocationFilterPolicy() { }

    static boolean shouldReinitialize(NavigationLocation previous, NavigationLocation in, double dt) {
        boolean courseChanged = trustedCourse(in) && trustedCourse(previous)
                && GeoMath.angularDiffDegrees(in.getBearing(), previous.getBearing()) > 25;
        return accuracy(in) * 2 < accuracy(previous) || courseChanged
                || (sparseInterval(dt) && sparseMotion(previous, in));
    }

    static boolean sparseInterval(double dt) {
        return dt >= SPARSE_INTERVAL_SECONDS;
    }

    private static boolean sparseMotion(NavigationLocation previous, NavigationLocation in) {
        return !(stationary(in) && stationary(previous))
                || previous.distanceTo(in) > accuracy(previous) + accuracy(in);
    }

    static boolean isValid(NavigationLocation in) {
        return Double.isFinite(in.getLatitude()) && Math.abs(in.getLatitude()) <= 90
                && Double.isFinite(in.getLongitude()) && Math.abs(in.getLongitude()) <= 180
                && (!in.hasAccuracy() || (Float.isFinite(in.getAccuracy()) && in.getAccuracy() >= 0));
    }

    static boolean validSpeed(NavigationLocation in) {
        return in.hasSpeed() && Float.isFinite(in.getSpeed()) && in.getSpeed() >= 0;
    }

    static double speed(NavigationLocation in) {
        return validSpeed(in) ? in.getSpeed() : 0;
    }

    static boolean stationary(NavigationLocation in) {
        return validSpeed(in) && in.getSpeed() <= STATIONARY_SPEED_MPS;
    }

    static boolean trustedCourse(NavigationLocation in) {
        return validSpeed(in) && in.getSpeed() > STATIONARY_SPEED_MPS
                && in.hasSpeedAccuracy() && Float.isFinite(in.getSpeedAccuracyMetersPerSecond())
                && in.getSpeedAccuracyMetersPerSecond() >= 0
                && in.getSpeedAccuracyMetersPerSecond() <= Math.max(0.5, in.getSpeed() / 2)
                && in.hasBearing() && Float.isFinite(in.getBearing())
                && in.hasBearingAccuracy() && Float.isFinite(in.getBearingAccuracyDegrees())
                && in.getBearingAccuracyDegrees() >= 0 && in.getBearingAccuracyDegrees() <= 25;
    }

    static double accuracy(NavigationLocation in) {
        return in.hasAccuracy() ? Math.max(0.5, in.getAccuracy()) : DEFAULT_ACCURACY_METERS;
    }

    static double measurementVariance(NavigationLocation in) {
        double accuracy = accuracy(in);
        return accuracy * accuracy / RADIAL_VARIANCE_FACTOR;
    }
}
