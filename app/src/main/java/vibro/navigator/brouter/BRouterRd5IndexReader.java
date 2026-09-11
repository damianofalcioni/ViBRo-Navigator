package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/** Reads and caches the small rd5 index structures shared by neighboring street chunks. */
final class BRouterRd5IndexReader {
    static final int TOP_INDEX_BYTES = 200;
    private static final int SUB_TILE_COUNT = 25;
    private static final int OLD_DIVISOR = 80;
    private static final int NEW_DIVISOR = 32;
    private static final int EXTRA_FOOTER_BASE_BYTES = 8 + 26 * 4;

    @NonNull
    private final BRouterSegmentReadFile file;
    @NonNull
    private final String sourceKey;
    @NonNull
    private final BRouterRd5MetadataCache metadataCache;
    @NonNull
    private final long[] topIndex = new long[SUB_TILE_COUNT];
    private int divisor = OLD_DIVISOR;
    private String cacheRevision = "";
    @Nullable
    private BRouterRd5MetadataCache.Entry metadata;

    BRouterRd5IndexReader(
            @NonNull BRouterSegmentReadFile file,
            @NonNull String sourceKey,
            @NonNull BRouterRd5MetadataCache metadataCache
    ) {
        this.file = file;
        this.sourceKey = sourceKey;
        this.metadataCache = metadataCache;
    }

    void read() throws IOException {
        long length = file.length();
        byte[] header = new byte[TOP_INDEX_BYTES];
        file.readFully(0L, header, 0, header.length);
        int headerCrc = Rd5Crc.crc(header, 0, header.length);
        long[] parsedTopIndex = parseTopIndex(header);
        Footer footer = readFooter(length, parsedTopIndex[parsedTopIndex.length - 1], headerCrc);
        cacheRevision = sourceKey + ":" + length + ":" + headerCrc + ":" + footer.crc;
        metadata = metadataCache.get(cacheRevision);
        if (metadata == null) {
            metadata = new BRouterRd5MetadataCache.Entry(parsedTopIndex, footer.divisor, cacheRevision);
            metadataCache.put(cacheRevision, metadata);
        }
        System.arraycopy(metadata.topIndex, 0, topIndex, 0, topIndex.length);
        divisor = metadata.divisor;
    }

    long topIndexAt(int index) {
        return topIndex[index];
    }

    int divisor() {
        return divisor;
    }

    @NonNull
    String cacheRevision() {
        return cacheRevision;
    }

    @NonNull
    int[] microCachePositions(int tileIndex, long fileOffset, int indexBytes, int count) throws IOException {
        if (metadata != null) {
            int[] cached = metadata.positionsFor(tileIndex);
            if (cached != null) {
                return cached;
            }
        }
        byte[] indexBuffer = new byte[indexBytes];
        file.readFully(fileOffset, indexBuffer, 0, indexBuffer.length);
        Rd5ByteDataReader reader = new Rd5ByteDataReader(indexBuffer);
        int[] positions = new int[count];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = reader.readInt();
        }
        if (metadata != null) {
            metadata.storePositions(tileIndex, positions);
        }
        return positions;
    }

    @NonNull
    private static long[] parseTopIndex(@NonNull byte[] header) {
        Rd5ByteDataReader reader = new Rd5ByteDataReader(header);
        long[] parsed = new long[SUB_TILE_COUNT];
        for (int i = 0; i < parsed.length; i++) {
            parsed[i] = reader.readLong() & 0xffffffffffffL;
        }
        return parsed;
    }

    @NonNull
    private Footer readFooter(long length, long footerPosition, int headerCrc) throws IOException {
        if (length <= footerPosition) {
            return new Footer(OLD_DIVISOR, 0);
        }
        int extraLength = resolveFooterLength(length, footerPosition);
        if (length < footerPosition + extraLength) {
            throw new IOException("rd5 footer is shorter than expected");
        }
        byte[] footer = new byte[extraLength];
        file.readFully(footerPosition, footer, 0, footer.length);
        int footerCrc = Rd5Crc.crc(footer, 0, footer.length);
        Rd5ByteDataReader reader = new Rd5ByteDataReader(footer);
        reader.readLong();
        int crcData = reader.readInt();
        return new Footer(resolveDivisor(headerCrc, crcData), footerCrc);
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

    private static final class Footer {
        final int divisor;
        final int crc;

        Footer(int divisor, int crc) {
            this.divisor = divisor;
            this.crc = crc;
        }
    }
}
