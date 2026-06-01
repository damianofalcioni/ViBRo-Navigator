package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

import java.util.ArrayDeque;

public final class NavigationLocationMotionModel {

    private static final float MAX_STATIONARY_REPORTED_SPEED_MPS = 0.35f;
    private static final long RECENT_MOTION_WINDOW_MS = 3_000L;
    private static final double MAX_STATIONARY_RECENT_DISTANCE_METERS = 0.8;
    private static final long MIN_MOVEMENT_BEARING_ELAPSED_MS = 2_000L;
    private static final double MIN_MOVEMENT_BEARING_DISTANCE_METERS = 3.0;

    private final ArrayDeque<NavigationLocation> recentFilteredLocations = new ArrayDeque<>();

    @Nullable
    private NavigationLocation lastFiltered;
    @Nullable
    private NavigationLocation previousFiltered;

    public void reset() {
        lastFiltered = null;
        previousFiltered = null;
        recentFilteredLocations.clear();
    }

    @Nullable
    public NavigationLocation getLastFilteredLocation() {
        return lastFiltered;
    }

    public void recordFilteredLocation(@NonNull NavigationLocation filtered) {
        previousFiltered = lastFiltered;
        lastFiltered = filtered;
        recentFilteredLocations.addLast(new NavigationLocation(filtered));
        pruneRecentFilteredLocations(filtered.getTime());
    }

    public boolean isLikelyStationary() {
        if (lastFiltered == null) {
            return false;
        }
        if (speedMps(lastFiltered) > MAX_STATIONARY_REPORTED_SPEED_MPS) {
            return false;
        }
        pruneRecentFilteredLocations(lastFiltered.getTime());
        if (recentFilteredLocations.size() < 2) {
            return true;
        }
        return totalRecentDistanceMeters() <= MAX_STATIONARY_RECENT_DISTANCE_METERS;
    }

    public float speedMps(@NonNull NavigationLocation location) {
        if (location.hasSpeed()) {
            return Math.max(0f, location.getSpeed());
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
        double deltaSeconds = Math.max(1.0, (location.getTime() - previousFiltered.getTime()) / 1000.0);
        return (float) (distanceMeters / deltaSeconds);
    }

    @Nullable
    public Double movementBearingDegrees(@NonNull NavigationLocation location) {
        pruneRecentFilteredLocations(location.getTime());
        for (NavigationLocation sample : recentFilteredLocations) {
            long elapsedMs = location.getTime() - sample.getTime();
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

    private double totalRecentDistanceMeters() {
        NavigationLocation previous = null;
        double cumulativeDistanceMeters = 0.0;
        for (NavigationLocation sample : recentFilteredLocations) {
            if (previous != null) {
                cumulativeDistanceMeters += GeoMath.distanceMeters(
                        previous.getLatitude(),
                        previous.getLongitude(),
                        sample.getLatitude(),
                        sample.getLongitude()
                );
            }
            previous = sample;
        }
        return cumulativeDistanceMeters;
    }

    private void pruneRecentFilteredLocations(long newestTimeMs) {
        long cutoffTimeMs = newestTimeMs - RECENT_MOTION_WINDOW_MS;
        while (recentFilteredLocations.size() > 1
                && recentFilteredLocations.peekFirst() != null
                && recentFilteredLocations.peekFirst().getTime() < cutoffTimeMs) {
            recentFilteredLocations.removeFirst();
        }
    }
}
