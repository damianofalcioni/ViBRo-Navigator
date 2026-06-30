package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
        List<KeyDistance> candidates = areaCandidates(center, selectionRadiusMeters);
        Collections.sort(candidates, new Comparator<KeyDistance>() {
            @Override
            public int compare(KeyDistance left, KeyDistance right) {
                return Double.compare(left.distanceMeters, right.distanceMeters);
            }
        });
        addCandidates(candidates, keys, maxKeys);
    }

    @NonNull
    private List<KeyDistance> areaCandidates(@NonNull LatLon center, double selectionRadiusMeters) {
        SurroundingStreetChunkKey centerKey = SurroundingStreetChunkKey.from(center);
        int latSpan = indexSpan(selectionRadiusMeters, METERS_PER_DEGREE);
        int lonSpan = indexSpan(selectionRadiusMeters, lonMetersPerDegree(center.lat));
        List<KeyDistance> candidates = new ArrayList<>();
        for (int latOffset = -latSpan; latOffset <= latSpan; latOffset++) {
            for (int lonOffset = -lonSpan; lonOffset <= lonSpan; lonOffset++) {
                addAreaCandidate(center, selectionRadiusMeters, centerKey, latOffset, lonOffset, candidates);
            }
        }
        return candidates;
    }

    private void addAreaCandidate(
            @NonNull LatLon center,
            double selectionRadiusMeters,
            @NonNull SurroundingStreetChunkKey centerKey,
            int latOffset,
            int lonOffset,
            @NonNull List<KeyDistance> out
    ) {
        SurroundingStreetChunkKey key = SurroundingStreetChunkKey.fromIndexes(
                centerKey.latIndex() + latOffset,
                centerKey.lonIndex() + lonOffset
        );
        double distanceMeters = key.distanceMetersTo(center);
        if (distanceMeters <= selectionRadiusMeters) {
            out.add(new KeyDistance(key, distanceMeters));
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
