package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

final class BRouterSegmentTile {
    static final int MICRO_DEGREES = 1_000_000;
    private static final int LON_OFFSET_DEGREES = 180;
    private static final int LAT_OFFSET_DEGREES = 90;
    private static final double MIN_LAT = -89.999999d;
    private static final double MAX_LAT = 89.999999d;
    private static final double MIN_LON = -179.999999d;
    private static final double MAX_LON = 179.999999d;

    private BRouterSegmentTile() {
    }

    static int integerLon(double lon) {
        return (int) Math.floor((clamp(lon, MIN_LON, MAX_LON) + LON_OFFSET_DEGREES) * MICRO_DEGREES);
    }

    static int integerLat(double lat) {
        return (int) Math.floor((clamp(lat, MIN_LAT, MAX_LAT) + LAT_OFFSET_DEGREES) * MICRO_DEGREES);
    }

    static double lonFromInteger(int ilon) {
        return ((double) ilon / MICRO_DEGREES) - LON_OFFSET_DEGREES;
    }

    static double latFromInteger(int ilat) {
        return ((double) ilat / MICRO_DEGREES) - LAT_OFFSET_DEGREES;
    }

    @NonNull
    static String fileNameFor(double lat, double lon) {
        return fileNameForIntegerDegrees(
                integerLon(lon) / MICRO_DEGREES,
                integerLat(lat) / MICRO_DEGREES
        );
    }

    @NonNull
    static String fileNameForIntegerDegrees(int lonDegree, int latDegree) {
        int lon = lonDegree - LON_OFFSET_DEGREES - positiveMod(lonDegree, 5);
        int lat = latDegree - LAT_OFFSET_DEGREES - positiveMod(latDegree, 5);
        return directionName(lon, "E", "W") + "_" + directionName(lat, "N", "S") + ".rd5";
    }

    @NonNull
    static Set<String> fileNamesForBounds(@NonNull BRouterSegmentBounds bounds) {
        Set<String> names = new LinkedHashSet<>();
        int minLonDegree = bounds.minIntegerLon / MICRO_DEGREES;
        int maxLonDegree = bounds.maxIntegerLon / MICRO_DEGREES;
        int minLatDegree = bounds.minIntegerLat / MICRO_DEGREES;
        int maxLatDegree = bounds.maxIntegerLat / MICRO_DEGREES;
        for (int lonDegree = minLonDegree; lonDegree <= maxLonDegree; lonDegree++) {
            for (int latDegree = minLatDegree; latDegree <= maxLatDegree; latDegree++) {
                names.add(fileNameForIntegerDegrees(lonDegree, latDegree));
            }
        }
        return names;
    }

    private static String directionName(int value, @NonNull String positivePrefix, @NonNull String negativePrefix) {
        return value < 0 ? negativePrefix + (-value) : positivePrefix + value;
    }

    private static int positiveMod(int value, int divisor) {
        int mod = value % divisor;
        return mod < 0 ? mod + divisor : mod;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
