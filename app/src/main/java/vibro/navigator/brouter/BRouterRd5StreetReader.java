package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetVisibility;

final class BRouterRd5StreetReader {
    @NonNull
    private final BRouterSegmentReadFile file;
    @NonNull
    private final String fileName;
    @NonNull
    private final BRouterDecodedStreetCache decodedCache;
    private final BRouterRd5IndexReader indexReader;

    BRouterRd5StreetReader(@NonNull BRouterSegmentReadFile file, @NonNull String fileName) {
        this(file, fileName, fileName, new BRouterDecodedStreetCache());
    }

    BRouterRd5StreetReader(
            BRouterSegmentReadFile file,
            String fileName,
            String sourceKey,
            BRouterDecodedStreetCache decodedCache
    ) {
        this(file, fileName, sourceKey, decodedCache, decodedCache.metadataCache());
    }

    BRouterRd5StreetReader(
            BRouterSegmentReadFile file,
            String fileName,
            String sourceKey,
            BRouterDecodedStreetCache decodedCache,
            BRouterRd5MetadataCache metadataCache
    ) {
        this.file = file;
        this.fileName = fileName;
        this.decodedCache = decodedCache;
        this.indexReader = new BRouterRd5IndexReader(file, sourceKey, metadataCache);
    }

    void read(
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) throws IOException {
        read(bounds, maxSegments, out, CompassStreetVisibility.all());
    }

    void read(
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out,
            @NonNull CompassStreetVisibility visibility
    ) throws IOException {
        BRouterStreetReadCancellation.check();
        if (out.size() >= maxSegments) {
            return;
        }
        BRouterStreetSegmentCollector collector = new BRouterStreetSegmentCollector(
                bounds, maxSegments - out.size(), visibility
        );
        indexReader.read();
        int minLonDegree = bounds.minIntegerLon / BRouterSegmentTile.MICRO_DEGREES;
        int maxLonDegree = bounds.maxIntegerLon / BRouterSegmentTile.MICRO_DEGREES;
        int minLatDegree = bounds.minIntegerLat / BRouterSegmentTile.MICRO_DEGREES;
        int maxLatDegree = bounds.maxIntegerLat / BRouterSegmentTile.MICRO_DEGREES;
        for (int lonDegree = minLonDegree; lonDegree <= maxLonDegree; lonDegree++) {
            for (int latDegree = minLatDegree; latDegree <= maxLatDegree; latDegree++) {
                if (fileName.equals(BRouterSegmentTile.fileNameForIntegerDegrees(lonDegree, latDegree))) {
                    readOneDegree(lonDegree, latDegree, bounds, collector);
                }
            }
        }
        collector.appendTo(out);
    }

    private void readOneDegree(
            int lonDegree,
            int latDegree,
            @NonNull BRouterSegmentBounds bounds,
            @NonNull BRouterStreetSegmentCollector collector
    ) throws IOException {
        int lonMod = positiveMod(lonDegree, 5);
        int latMod = positiveMod(latDegree, 5);
        int tileIndex = lonMod * 5 + latMod;
        long fileOffset = tileIndex > 0
                ? indexReader.topIndexAt(tileIndex - 1)
                : BRouterRd5IndexReader.TOP_INDEX_BYTES;
        if (fileOffset == indexReader.topIndexAt(tileIndex)) {
            return;
        }
        int divisor = indexReader.divisor();
        int cacheCount = divisor * divisor;
        int indexBytes = cacheCount * 4;
        int[] positions = indexReader.microCachePositions(tileIndex, fileOffset, indexBytes, cacheCount);
        int cellSize = BRouterSegmentTile.MICRO_DEGREES / divisor;
        int minLonIndex = Math.max(divisor * lonDegree, bounds.minIntegerLon / cellSize);
        int maxLonIndex = Math.min(divisor * lonDegree + divisor - 1, bounds.maxIntegerLon / cellSize);
        int minLatIndex = Math.max(divisor * latDegree, bounds.minIntegerLat / cellSize);
        int maxLatIndex = Math.min(divisor * latDegree + divisor - 1, bounds.maxIntegerLat / cellSize);
        for (int lonIndex = minLonIndex; lonIndex <= maxLonIndex; lonIndex++) {
            for (int latIndex = minLatIndex; latIndex <= maxLatIndex; latIndex++) {
                readMicroCache(fileOffset, indexBytes, positions, lonDegree, latDegree,
                        lonIndex, latIndex, divisor, bounds, collector);
            }
        }
    }

    private void readMicroCache(
            long fileOffset,
            int indexBytes,
            @NonNull int[] positions,
            int lonDegree,
            int latDegree,
            int lonIndex,
            int latIndex,
            int divisor,
            @NonNull BRouterSegmentBounds bounds,
            @NonNull BRouterStreetSegmentCollector collector
    ) throws IOException {
        int subIndex = (latIndex - divisor * latDegree) * divisor + (lonIndex - divisor * lonDegree);
        int start = subIndex == 0 ? indexBytes : positions[subIndex - 1];
        int end = positions[subIndex];
        int size = end - start;
        if (size <= 0) {
            return;
        }
        readCell(fileOffset + start, size, lonIndex, latIndex, divisor, bounds, collector);
    }

    private void readCell(
            long position, int size, int lonIndex, int latIndex,
            int divisor,
            BRouterSegmentBounds bounds, BRouterStreetSegmentCollector collector
    ) throws IOException {
        BRouterStreetReadCancellation.check();
        String key = indexReader.cacheRevision() + ":" + position + ":" + size;
        BRouterPackedStreetCell cached = decodedCache.get(key);
        if (cached != null) {
            cached.collect(bounds, collector);
            return;
        }
        byte[] data = new byte[size];
        file.readFully(position, data, 0, data.length);
        verifyChecksumIfNeeded(data);
        BRouterPackedStreetCell.Builder packed = new BRouterPackedStreetCell.Builder(decodedCache.maxCellBytes());
        new BRouterRd5MicroCacheStreetDecoder(lonIndex, latIndex, divisor, (points, type) -> {
            collector.offer(points, type);
            packed.offer(points, type);
        }).decode(data);
        BRouterStreetReadCancellation.check();
        decodedCache.put(key, packed.build());
    }

    private static void verifyChecksumIfNeeded(@NonNull byte[] data) throws IOException {
        if (data.length < 4) {
            return;
        }
        int crcData = Rd5Crc.crc(data, 0, data.length - 4);
        int crcFooter = new Rd5ByteDataReader(data, data.length - 4).readInt();
        if (crcData == crcFooter) {
            throw new IOException("old unsupported rd5 microcache format");
        } else if ((crcData ^ 2) != crcFooter) {
            throw new IOException("rd5 microcache checksum mismatch");
        }
    }

    private static int positiveMod(int value, int divisor) {
        int mod = value % divisor;
        return mod < 0 ? mod + divisor : mod;
    }
}
