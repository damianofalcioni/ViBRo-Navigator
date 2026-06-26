package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;

// Adapted from BRouter btools.codec.MicroCache2 (MIT), emitting only forward geometry lines.
final class BRouterRd5MicroCacheStreetDecoder {
    private final int lonBase;
    private final int latBase;
    private final int cellSize;
    @NonNull
    private final BRouterSegmentBounds bounds;
    private final int maxSegments;
    @NonNull
    private final List<CompassStreetSegment> out;

    BRouterRd5MicroCacheStreetDecoder(
            int lonIndex,
            int latIndex,
            int divisor,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) {
        cellSize = BRouterSegmentTile.MICRO_DEGREES / divisor;
        lonBase = lonIndex * cellSize;
        latBase = latIndex * cellSize;
        this.bounds = bounds;
        this.maxSegments = maxSegments;
        this.out = out;
    }

    void decode(@NonNull byte[] data) {
        Rd5StatCoderContext context = new Rd5StatCoderContext(data);
        Rd5TagValueCoder wayTagCoder = new Rd5TagValueCoder(context);
        Rd5TagValueCoder nodeTagCoder = new Rd5TagValueCoder(context);
        Rd5NoisyDiffCoder nodeIndexDiff = new Rd5NoisyDiffCoder(context);
        Rd5NoisyDiffCoder nodeEleDiff = new Rd5NoisyDiffCoder(context);
        Rd5NoisyDiffCoder externalLonDiff = new Rd5NoisyDiffCoder(context);
        Rd5NoisyDiffCoder externalLatDiff = new Rd5NoisyDiffCoder(context);
        Rd5NoisyDiffCoder transferEleDiff = new Rd5NoisyDiffCoder(context);

        int nodeCount = context.decodeNoisyNumber(5);
        int[] nodeIds = new int[nodeCount];
        int[] lons = new int[nodeCount];
        int[] lats = new int[nodeCount];
        context.decodeSortedArray(nodeIds, 0, nodeCount, 29, 0);
        for (int i = 0; i < nodeCount; i++) {
            long id = expandId(nodeIds[i]);
            lons[i] = (int) (id >> 32);
            lats[i] = (int) id;
        }
        context.decodeNoisyNumber(10);
        decodeNodes(context, nodeTagCoder, wayTagCoder, nodeIndexDiff, nodeEleDiff,
                externalLonDiff, externalLatDiff, transferEleDiff, lons, lats);
    }

    private void decodeNodes(
            @NonNull Rd5StatCoderContext context,
            @NonNull Rd5TagValueCoder nodeTagCoder,
            @NonNull Rd5TagValueCoder wayTagCoder,
            @NonNull Rd5NoisyDiffCoder nodeIndexDiff,
            @NonNull Rd5NoisyDiffCoder nodeEleDiff,
            @NonNull Rd5NoisyDiffCoder externalLonDiff,
            @NonNull Rd5NoisyDiffCoder externalLatDiff,
            @NonNull Rd5NoisyDiffCoder transferEleDiff,
            @NonNull int[] lons,
            @NonNull int[] lats
    ) {
        for (int nodeIndex = 0; nodeIndex < lons.length && out.size() < maxSegments; nodeIndex++) {
            int sourceLon = lons[nodeIndex];
            int sourceLat = lats[nodeIndex];
            skipNodeFeatures(context);
            nodeEleDiff.decodeSignedValue();
            nodeTagCoder.decodeTagValueSet();
            int linkCount = context.decodeNoisyNumber(1);
            for (int linkIndex = 0; linkIndex < linkCount && out.size() < maxSegments; linkIndex++) {
                decodeLink(
                        context,
                        wayTagCoder,
                        nodeIndexDiff,
                        externalLonDiff,
                        externalLatDiff,
                        transferEleDiff,
                        lons,
                        lats,
                        nodeIndex,
                        sourceLon,
                        sourceLat
                );
            }
        }
    }

    private void skipNodeFeatures(@NonNull Rd5StatCoderContext context) {
        int featureId = context.decodeVarBits();
        while (featureId != 0) {
            int bits = context.decodeNoisyNumber(5);
            if (featureId == 2) {
                context.decodeBounded(1023);
            } else if (featureId == 1) {
                context.decodeBit();
                context.decodeNoisyDiff(10);
                context.decodeNoisyDiff(10);
                context.decodeNoisyDiff(10);
                context.decodeNoisyDiff(10);
            } else {
                skipBits(context, bits);
            }
            featureId = context.decodeVarBits();
        }
    }

    private void decodeLink(
            @NonNull Rd5StatCoderContext context,
            @NonNull Rd5TagValueCoder wayTagCoder,
            @NonNull Rd5NoisyDiffCoder nodeIndexDiff,
            @NonNull Rd5NoisyDiffCoder externalLonDiff,
            @NonNull Rd5NoisyDiffCoder externalLatDiff,
            @NonNull Rd5NoisyDiffCoder transferEleDiff,
            @NonNull int[] lons,
            @NonNull int[] lats,
            int sourceIndex,
            int sourceLon,
            int sourceLat
    ) {
        int targetIndex = sourceIndex + nodeIndexDiff.decodeSignedValue();
        int remainingLon;
        int remainingLat;
        boolean reverse = false;
        if (targetIndex != sourceIndex) {
            remainingLon = lons[targetIndex] - sourceLon;
            remainingLat = lats[targetIndex] - sourceLat;
        } else {
            reverse = context.decodeBit();
            remainingLon = externalLonDiff.decodeSignedValue();
            remainingLat = externalLatDiff.decodeSignedValue();
        }
        wayTagCoder.decodeTagValueSet();
        if (reverse) {
            return;
        }
        int targetLon = sourceLon + remainingLon;
        int targetLat = sourceLat + remainingLat;
        List<LatLon> points = decodeForwardGeometry(
                context,
                transferEleDiff,
                sourceLon,
                sourceLat,
                targetLon,
                targetLat,
                remainingLon,
                remainingLat
        );
        if (points.size() >= 2 && bounds.intersects(points)) {
            out.add(new CompassStreetSegment(points));
        }
    }

    @NonNull
    private List<LatLon> decodeForwardGeometry(
            @NonNull Rd5StatCoderContext context,
            @NonNull Rd5NoisyDiffCoder transferEleDiff,
            int sourceLon,
            int sourceLat,
            int targetLon,
            int targetLat,
            int remainingLon,
            int remainingLat
    ) {
        int transferCount = context.decodeVarBits();
        int divisorCount = transferCount + 1;
        int lonRemaining = remainingLon;
        int latRemaining = remainingLat;
        List<LatLon> points = new ArrayList<>(transferCount + 2);
        points.add(toLatLon(sourceLon, sourceLat));
        for (int transferIndex = 0; transferIndex < transferCount; transferIndex++) {
            int dlon = context.decodePredictedValue(lonRemaining / divisorCount);
            int dlat = context.decodePredictedValue(latRemaining / divisorCount);
            lonRemaining -= dlon;
            latRemaining -= dlat;
            divisorCount--;
            transferEleDiff.decodeSignedValue();
            points.add(toLatLon(targetLon - lonRemaining, targetLat - latRemaining));
        }
        points.add(toLatLon(targetLon, targetLat));
        return points;
    }

    @NonNull
    private static LatLon toLatLon(int integerLon, int integerLat) {
        return new LatLon(
                BRouterSegmentTile.latFromInteger(integerLat),
                BRouterSegmentTile.lonFromInteger(integerLon)
        );
    }

    private long expandId(int id32) {
        int lon = 0;
        int lat = 0;
        int value = id32;
        for (int bitMask = 1; bitMask < 0x8000; bitMask <<= 1) {
            if ((value & 1) != 0) {
                lon |= bitMask;
            }
            if ((value & 2) != 0) {
                lat |= bitMask;
            }
            value >>= 2;
        }
        return ((long) (lonBase + lon)) << 32 | (latBase + lat);
    }

    private static void skipBits(@NonNull Rd5BitCoderContext context, int bits) {
        for (int i = 0; i < bits; i++) {
            context.decodeBit();
        }
    }
}
