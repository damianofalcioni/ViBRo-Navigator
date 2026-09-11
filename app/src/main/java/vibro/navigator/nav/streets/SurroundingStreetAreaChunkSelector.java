package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import vibro.navigator.geo.LatLon;

final class SurroundingStreetAreaChunkSelector {
    private static final double METERS_PER_DEGREE = 111_320.0d;

    void addAreaKeys(
            @NonNull LatLon center,
            float cacheRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        if (!Float.isFinite(cacheRadiusMeters) || cacheRadiusMeters <= 0f || keys.size() >= maxKeys) {
            return;
        }
        double selectionRadiusMeters = cacheRadiusMeters + SurroundingStreetChunkKey.LOAD_RADIUS_METERS;
        int remaining = maxKeys - keys.size();
        List<KeyDistance> selected = nearestCandidates(center, selectionRadiusMeters, keys, remaining);
        Collections.sort(selected, (left, right) ->
                Double.compare(left.distanceMeters, right.distanceMeters));
        addCandidates(selected, keys, maxKeys);
    }

    @NonNull
    private List<KeyDistance> nearestCandidates(
            @NonNull LatLon center,
            double selectionRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int remaining
    ) {
        PriorityQueue<KeyDistance> nearest = new PriorityQueue<>(remaining, (left, right) ->
                Double.compare(right.distanceMeters, left.distanceMeters));
        SurroundingStreetChunkKey centerKey = SurroundingStreetChunkKey.from(center);
        int latSpan = indexSpan(selectionRadiusMeters, METERS_PER_DEGREE);
        int lonSpan = indexSpan(selectionRadiusMeters, lonMetersPerDegree(center.lat));
        for (int latOffset = -latSpan; latOffset <= latSpan; latOffset++) {
            for (int lonOffset = -lonSpan; lonOffset <= lonSpan; lonOffset++) {
                SurroundingStreetChunkKey key = SurroundingStreetChunkKey.fromIndexes(
                        centerKey.latIndex() + latOffset,
                        centerKey.lonIndex() + lonOffset
                );
                considerCandidate(center, selectionRadiusMeters, keys, key, remaining, nearest);
            }
        }
        return new ArrayList<>(nearest);
    }

    private void considerCandidate(
            @NonNull LatLon center,
            double selectionRadiusMeters,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            @NonNull SurroundingStreetChunkKey key,
            int remaining,
            @NonNull PriorityQueue<KeyDistance> nearest
    ) {
        if (keys.contains(key)) {
            return;
        }
        double distanceMeters = key.distanceMetersTo(center);
        if (distanceMeters > selectionRadiusMeters) {
            return;
        }
        if (nearest.size() < remaining) {
            nearest.add(new KeyDistance(key, distanceMeters));
        } else if (distanceMeters < nearest.peek().distanceMeters) {
            nearest.poll();
            nearest.add(new KeyDistance(key, distanceMeters));
        }
    }

    private void addCandidates(
            @NonNull List<KeyDistance> candidates,
            @NonNull Set<SurroundingStreetChunkKey> keys,
            int maxKeys
    ) {
        for (KeyDistance candidate : candidates) {
            keys.add(candidate.key);
            if (keys.size() >= maxKeys) {
                return;
            }
        }
    }

    private static int indexSpan(double radiusMeters, double metersPerDegree) {
        double chunkMeters = SurroundingStreetChunkKey.CELL_SIZE_DEGREES * metersPerDegree;
        return Math.max(1, (int) Math.ceil(radiusMeters / chunkMeters));
    }

    private static double lonMetersPerDegree(double latitude) {
        return Math.max(1_000.0d, METERS_PER_DEGREE * Math.abs(Math.cos(Math.toRadians(latitude))));
    }

    private static final class KeyDistance {
        @NonNull
        final SurroundingStreetChunkKey key;
        final double distanceMeters;

        KeyDistance(@NonNull SurroundingStreetChunkKey key, double distanceMeters) {
            this.key = key;
            this.distanceMeters = distanceMeters;
        }
    }
}
