package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import vibro.navigator.nav.compass.CompassStreetSegment;

final class BRouterRd5StreetReader {
    private static final int TOP_INDEX_BYTES = 200;
    private static final int SUB_TILE_COUNT = 25;
    private static final int OLD_DIVISOR = 80;
    private static final int NEW_DIVISOR = 32;
    private static final int EXTRA_FOOTER_BASE_BYTES = 8 + 26 * 4;

    @NonNull
    private final BRouterSegmentReadFile file;
    @NonNull
    private final String fileName;
    @NonNull
    private final long[] topIndex = new long[SUB_TILE_COUNT];
    private int divisor = OLD_DIVISOR;

    BRouterRd5StreetReader(@NonNull BRouterSegmentReadFile file, @NonNull String fileName) {
        this.file = file;
        this.fileName = fileName;
    }

    void read(
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) throws IOException {
        readTopIndex();
        int minLonDegree = bounds.minIntegerLon / BRouterSegmentTile.MICRO_DEGREES;
        int maxLonDegree = bounds.maxIntegerLon / BRouterSegmentTile.MICRO_DEGREES;
        int minLatDegree = bounds.minIntegerLat / BRouterSegmentTile.MICRO_DEGREES;
        int maxLatDegree = bounds.maxIntegerLat / BRouterSegmentTile.MICRO_DEGREES;
        for (int lonDegree = minLonDegree; lonDegree <= maxLonDegree && out.size() < maxSegments; lonDegree++) {
            for (int latDegree = minLatDegree; latDegree <= maxLatDegree && out.size() < maxSegments; latDegree++) {
                if (fileName.equals(BRouterSegmentTile.fileNameForIntegerDegrees(lonDegree, latDegree))) {
                    readOneDegree(lonDegree, latDegree, bounds, maxSegments, out);
                }
            }
        }
    }

    private void readTopIndex() throws IOException {
        byte[] header = new byte[TOP_INDEX_BYTES];
        file.readFully(0L, header, 0, header.length);
        int headerCrc = Rd5Crc.crc(header, 0, header.length);
        Rd5ByteDataReader reader = new Rd5ByteDataReader(header);
        for (int i = 0; i < topIndex.length; i++) {
            topIndex[i] = reader.readLong() & 0xffffffffffffL;
        }
        readDivisor(headerCrc);
    }

    private void readDivisor(int headerCrc) throws IOException {
        long length = file.length();
        long footerPosition = topIndex[topIndex.length - 1];
        if (length <= footerPosition) {
            divisor = OLD_DIVISOR;
            return;
        }
        int extraLength = resolveFooterLength(length, footerPosition);
        if (length < footerPosition + extraLength) {
            throw new IOException("rd5 footer is shorter than expected");
        }
        byte[] footer = new byte[extraLength];
        file.readFully(footerPosition, footer, 0, footer.length);
        Rd5ByteDataReader reader = new Rd5ByteDataReader(footer);
        reader.readLong();
        int crcData = reader.readInt();
        divisor = resolveDivisor(headerCrc, crcData);
    }

    private static int resolveFooterLength(long length, long footerPosition) {
        return (length - footerPosition) > EXTRA_FOOTER_BASE_BYTES
                ? EXTRA_FOOTER_BASE_BYTES + 1
                : EXTRA_FOOTER_BASE_BYTES;
    }

    private static int resolveDivisor(int headerCrc, int crcData) throws IOException {
        if (crcData == headerCrc) {
            return OLD_DIVISOR;
        }
        if ((crcData ^ 2) == headerCrc) {
            return NEW_DIVISOR;
        }
        throw new IOException("rd5 top index checksum mismatch");
    }

    private void readOneDegree(
            int lonDegree,
            int latDegree,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) throws IOException {
        int lonMod = positiveMod(lonDegree, 5);
        int latMod = positiveMod(latDegree, 5);
        int tileIndex = lonMod * 5 + latMod;
        long fileOffset = tileIndex > 0 ? topIndex[tileIndex - 1] : TOP_INDEX_BYTES;
        if (fileOffset == topIndex[tileIndex]) {
            return;
        }
        int cacheCount = divisor * divisor;
        int indexBytes = cacheCount * 4;
        int[] positions = readMicroCachePositions(fileOffset, indexBytes, cacheCount);
        int cellSize = BRouterSegmentTile.MICRO_DEGREES / divisor;
        int minLonIndex = Math.max(divisor * lonDegree, bounds.minIntegerLon / cellSize);
        int maxLonIndex = Math.min(divisor * lonDegree + divisor - 1, bounds.maxIntegerLon / cellSize);
        int minLatIndex = Math.max(divisor * latDegree, bounds.minIntegerLat / cellSize);
        int maxLatIndex = Math.min(divisor * latDegree + divisor - 1, bounds.maxIntegerLat / cellSize);
        for (int lonIndex = minLonIndex; lonIndex <= maxLonIndex && out.size() < maxSegments; lonIndex++) {
            for (int latIndex = minLatIndex; latIndex <= maxLatIndex && out.size() < maxSegments; latIndex++) {
                readMicroCache(fileOffset, indexBytes, positions, lonDegree, latDegree,
                        lonIndex, latIndex, bounds, maxSegments, out);
            }
        }
    }

    @NonNull
    private int[] readMicroCachePositions(long fileOffset, int indexBytes, int count) throws IOException {
        byte[] indexBuffer = new byte[indexBytes];
        file.readFully(fileOffset, indexBuffer, 0, indexBuffer.length);
        Rd5ByteDataReader reader = new Rd5ByteDataReader(indexBuffer);
        int[] positions = new int[count];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = reader.readInt();
        }
        return positions;
    }

    private void readMicroCache(
            long fileOffset,
            int indexBytes,
            @NonNull int[] positions,
            int lonDegree,
            int latDegree,
            int lonIndex,
            int latIndex,
            @NonNull BRouterSegmentBounds bounds,
            int maxSegments,
            @NonNull List<CompassStreetSegment> out
    ) throws IOException {
        int subIndex = (latIndex - divisor * latDegree) * divisor + (lonIndex - divisor * lonDegree);
        int start = subIndex == 0 ? indexBytes : positions[subIndex - 1];
        int end = positions[subIndex];
        int size = end - start;
        if (size <= 0) {
            return;
        }
        byte[] data = new byte[size];
        file.readFully(fileOffset + start, data, 0, data.length);
        new BRouterRd5MicroCacheStreetDecoder(lonIndex, latIndex, divisor, bounds, maxSegments, out)
                .decode(data);
        verifyChecksumIfNeeded(data);
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
