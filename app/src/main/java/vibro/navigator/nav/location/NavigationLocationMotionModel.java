package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

import java.util.ArrayDeque;
import java.util.Objects;

public final class NavigationLocationMotionModel {

    private static final long RECENT_MOTION_WINDOW_MS = 3_000L;
    private static final long MIN_MOVEMENT_BEARING_ELAPSED_MS = 2_000L;
    private static final double MIN_MOVEMENT_BEARING_DISTANCE_METERS = 3.0;

    private final ArrayDeque<NavigationLocation> recentFilteredLocations = new ArrayDeque<>();
    private final NavigationRawMotionHistory rawHistory = new NavigationRawMotionHistory();
    private final NavigationDisplayCourseHistory displayCourseHistory = new NavigationDisplayCourseHistory();
    private final NavigationStationarityTracker stationarityTracker = new NavigationStationarityTracker();
    private final NavigationMovementEvidence movementEvidence = new NavigationMovementEvidence();

    @Nullable
    private NavigationLocation lastFiltered;
    @Nullable
    private NavigationLocation previousFiltered;

    public void reset() {
        lastFiltered = null;
        previousFiltered = null;
        recentFilteredLocations.clear();
        rawHistory.reset();
        displayCourseHistory.reset();
        stationarityTracker.reset();
        movementEvidence.reset();
    }

    @Nullable
    public NavigationLocation getLastFilteredLocation() {
        return lastFiltered;
    }

    public void recordFilteredLocation(@NonNull NavigationLocation filtered) {
        recordLocation(filtered, filtered);
    }

    public void recordLocation(@NonNull NavigationLocation raw, @NonNull NavigationLocation filtered) {
        if (!rawHistory.isNew(raw)) {
            if (!improvesCurrentObservation(raw)) {
                return;
            }
            // Keep an accepted position correction, but do not reuse confidence or
            // connect travel across the observation it replaces. Seed one observation.
            reset();
        }
        boolean providerChanged = lastFiltered != null && !Objects.equals(lastFiltered.getProvider(), filtered.getProvider());
        if (providerChanged) {
            // Provider offsets are position corrections, not evidence of speed or course.
            lastFiltered = null;
            recentFilteredLocations.clear();
            rawHistory.reset();
            displayCourseHistory.reset();
            movementEvidence.reset();
        }
        boolean wasStationary = stationarityTracker.isStationary();
        previousFiltered = lastFiltered;
        rawHistory.record(raw);
        lastFiltered = filtered;
        recentFilteredLocations.addLast(new NavigationLocation(filtered));
        pruneRecentFilteredLocations(filtered.getElapsedRealtimeOrTimeMs());
        if (providerChanged) {
            stationarityTracker.recordProviderChange(raw);
        } else {
            stationarityTracker.record(raw, rawHistory.isStationaryCandidate(), movementEvidence.record(raw));
        }
        if (!wasStationary && stationarityTracker.isStationary()) {
            // A departure may reverse direction. Approach fixes must not yield a confident
            // incoming course while the first return steps are still near the stop.
            displayCourseHistory.reset();
        }
        displayCourseHistory.record(filtered);
    }

    private boolean improvesCurrentObservation(NavigationLocation raw) {
        return lastFiltered != null
                && raw.getElapsedRealtimeOrTimeMs() == lastFiltered.getElapsedRealtimeOrTimeMs()
                && raw.hasAccuracy() && (!lastFiltered.hasAccuracy()
                || raw.getProviderAccuracy() < lastFiltered.getProviderAccuracy());
    }

    public boolean isLikelyStationary() {
        return stationarityTracker.isStationary();
    }

    public boolean needsDepartureChecks() {
        return stationarityTracker.needsDepartureChecks();
    }

    public NavigationMotionState motionState() { return stationarityTracker.state(); }

    /** Read-time expiry uses the caller's monotonic clock without scheduling a wake-up. */
    public NavigationMotionState motionState(long nowMs) { return stationarityTracker.state(nowMs); }

    public String motionReason() { return stationarityTracker.reason(); }

    public float speedMps(@NonNull NavigationLocation location) {
        pruneRecentFilteredLocations(location.getElapsedRealtimeOrTimeMs());
        if (isLikelyStationary() || isUncertainSpeed(location)) {
            return 0f;
        }
        return reportedOrFallbackSpeedMps(location);
    }

    private boolean isUncertainSpeed(NavigationLocation location) {
        boolean poorPosition = !location.hasAccuracy() || !Float.isFinite(location.getProviderAccuracy())
                || location.getProviderAccuracy() > 25;
        return motionState() == NavigationMotionState.UNKNOWN && (poorPosition || rawHistory.hasStationaryPositions())
                && !location.hasSpeedAccuracy();
    }

    public float displaySpeedMps(@NonNull NavigationLocation location) {
        pruneRecentFilteredLocations(location.getElapsedRealtimeOrTimeMs());
        return NavigationDisplaySpeedResolver.resolve(
                location,
                movementEvidence.reference() == null ? previousFiltered : movementEvidence.reference(),
                isLikelyStationary(),
                motionState().isMoving(),
                speedMps(location)
        );
    }

    private float reportedOrFallbackSpeedMps(@NonNull NavigationLocation location) {
        if (location.hasSpeed()) {
            float reportedSpeedMps = location.getSpeed();
            return Float.isFinite(reportedSpeedMps) && reportedSpeedMps > 0f ? reportedSpeedMps : 0f;
        }
        if (previousFiltered == null) {
            return 0f;
        }
        double distanceMeters = GeoMath.distanceMeters(
                previousFiltered.getLatitude(),
                previousFiltered.getLongitude(),
                location.getLatitude(),
                location.getLongitude()
        );
        double deltaSeconds = Math.max(
                1.0,
                (location.getElapsedRealtimeOrTimeMs() - previousFiltered.getElapsedRealtimeOrTimeMs()) / 1000.0
        );
        return (float) (distanceMeters / deltaSeconds);
    }

    @Nullable
    public Double movementBearingDegrees(@NonNull NavigationLocation location) {
        pruneRecentFilteredLocations(location.getElapsedRealtimeOrTimeMs());
        return movementBearingDegrees(location, recentFilteredLocations);
    }

    @Nullable
    public Course displayMovementCourse(@NonNull NavigationLocation location) {
        return displayCourseHistory.estimate(location);
    }

    public static final class Course {
        public final double headingDegrees;
        public final float accuracyDegrees;

        Course(double headingDegrees, float accuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.accuracyDegrees = accuracyDegrees;
        }
    }

    @Nullable
    private Double movementBearingDegrees(NavigationLocation location, ArrayDeque<NavigationLocation> samples) {
        for (NavigationLocation sample : samples) {
            long elapsedMs = location.getElapsedRealtimeOrTimeMs() - sample.getElapsedRealtimeOrTimeMs();
            if (elapsedMs < MIN_MOVEMENT_BEARING_ELAPSED_MS) {
                continue;
            }
            double distanceMeters = GeoMath.distanceMeters(
                    sample.getLatitude(),
                    sample.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            if (distanceMeters < MIN_MOVEMENT_BEARING_DISTANCE_METERS) {
                continue;
            }
            return GeoMath.bearingDegrees(
                    sample.getLatitude(),
                    sample.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
        }
        return null;
    }

    private void pruneRecentFilteredLocations(long newestTimeMs) {
        long cutoffTimeMs = newestTimeMs - RECENT_MOTION_WINDOW_MS;
        while (recentFilteredLocations.size() > 1
                && recentFilteredLocations.peekFirst() != null
                && recentFilteredLocations.peekFirst().getElapsedRealtimeOrTimeMs() < cutoffTimeMs) {
            recentFilteredLocations.removeFirst();
        }
    }

}
