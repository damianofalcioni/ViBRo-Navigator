package vibro.navigator.brouter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetType;

/** Compact immutable geometry: type, point count, bounds, then integer latitude/longitude pairs. */
final class BRouterPackedStreetCell {
    private static final int BYTES_PER_WORD = 4;
    private static final int HEADER_WORDS = 6;
    private static final CompassStreetType[] TYPES = CompassStreetType.values();
    private final int[] words;

    private BRouterPackedStreetCell(int[] words) {
        this.words = words;
    }

    int byteSize() {
        return words.length * BYTES_PER_WORD;
    }

    void collect(BRouterSegmentBounds bounds, BRouterStreetGeometrySink sink) {
        int offset = 0;
        while (offset < words.length) {
            BRouterStreetReadCancellation.check();
            int count = words[offset + 1];
            if (intersects(offset, bounds)) {
                sink.offer(points(offset + HEADER_WORDS, count), TYPES[words[offset]]);
            }
            offset += HEADER_WORDS + count * 2;
        }
    }

    private boolean intersects(int offset, BRouterSegmentBounds bounds) {
        return words[offset + 2] <= bounds.maxIntegerLat + 1
                && words[offset + 3] >= bounds.minIntegerLat - 1
                && words[offset + 4] <= bounds.maxIntegerLon + 1
                && words[offset + 5] >= bounds.minIntegerLon - 1;
    }

    private List<LatLon> points(int offset, int count) {
        List<LatLon> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new LatLon(
                    BRouterSegmentTile.latFromInteger(words[offset + i * 2]),
                    BRouterSegmentTile.lonFromInteger(words[offset + i * 2 + 1])
            ));
        }
        return points;
    }

    static final class Builder implements BRouterStreetGeometrySink {
        private final int maxWords;
        private int[] words;
        private int size;

        Builder(int maxBytes) {
            maxWords = Math.max(0, maxBytes / BYTES_PER_WORD);
            words = new int[Math.min(1024, maxWords)];
        }

        @Override
        public void offer(List<LatLon> points, CompassStreetType type) {
            if (!reserve(HEADER_WORDS + points.size() * 2)) {
                return;
            }
            int header = size;
            words[size++] = type.ordinal();
            words[size++] = points.size();
            words[size++] = Integer.MAX_VALUE;
            words[size++] = Integer.MIN_VALUE;
            words[size++] = Integer.MAX_VALUE;
            words[size++] = Integer.MIN_VALUE;
            for (LatLon point : points) {
                appendPoint(header, point);
            }
        }

        private void appendPoint(int header, LatLon point) {
            int lat = (int) Math.round((point.lat + 90d) * BRouterSegmentTile.MICRO_DEGREES);
            int lon = (int) Math.round((point.lon + 180d) * BRouterSegmentTile.MICRO_DEGREES);
            words[header + 2] = Math.min(words[header + 2], lat);
            words[header + 3] = Math.max(words[header + 3], lat);
            words[header + 4] = Math.min(words[header + 4], lon);
            words[header + 5] = Math.max(words[header + 5], lon);
            words[size++] = lat;
            words[size++] = lon;
        }

        private boolean reserve(int additionalWords) {
            if (words == null) {
                return false;
            }
            int required = size + additionalWords;
            if (required > maxWords) {
                words = null; // Never retain a partial cell; the live query still receives every street.
                return false;
            }
            if (required > words.length) {
                words = Arrays.copyOf(words, Math.min(maxWords, Math.max(required, words.length * 2)));
            }
            return true;
        }

        BRouterPackedStreetCell build() {
            return words == null ? null : new BRouterPackedStreetCell(Arrays.copyOf(words, size));
        }
    }
}
