package vibro.navigator.brouter;

import java.nio.ByteBuffer;
import java.util.Base64;

/** Synthetic roads encoded with BRouter's MicroCache2 encoder, without map data or runtime dependencies. */
final class BRouterStreetTestFile implements BRouterSegmentReadFile {
    // Each cell has 16 residential links: 8 west and 8 east of 16.381475 degrees.
    // Southern starts: lat 48.187100 + i*0.000013; northern: 48.187900 + i*0.000013.
    // Longitudes: 16.380900 / 16.381800 + i*0.000013, i=0..7; endpoints 5 microdegrees east.
    // Encoder: https://github.com/abrensch/brouter/blob/master/brouter-codec/src/main/java/btools/codec/MicroCache2.java
    private static final byte[] SOUTH = Base64.getDecoder().decode(
            "tJNhAAEEACACAgJguda4rqXhRcGmXaR2RAREgHIl4t1J04uDHbpK55AMLF3kL/IX+Yv8Rf4if5G/yF/kL/IX+Yv8Rf4if5G/yIpRah4="
    );
    private static final byte[] NORTH = Base64.getDecoder().decode(
            "tJNhCCGEACICAgAgqVQ4ppz0+BBUvkk+IgIgAKkQcKaZ/PgwEPzJPCQDSxf5i/xF/iJ/kb/IX+Qv8hf5i/xF/iJ/kb/IX+QvMnjXKPY="
    );
    private static final int HEADER_BYTES = 200;
    private static final int INDEX_BYTES = 32 * 32 * 4;
    private final byte[] data;
    int cellReads;

    BRouterStreetTestFile() {
        int dataEnd = HEADER_BYTES + INDEX_BYTES + SOUTH.length + NORTH.length;
        ByteBuffer file = ByteBuffer.allocate(dataEnd + 112);
        for (int tile = 0; tile < 25; tile++) {
            file.putLong(tile < 8 ? HEADER_BYTES : dataEnd);
        }
        int headerCrc = Rd5Crc.crc(file.array(), 0, HEADER_BYTES);
        int position = INDEX_BYTES;
        for (int cell = 0; cell < 32 * 32; cell++) {
            if (cell == 172) {
                position += SOUTH.length;
            }
            if (cell == 204) {
                position += NORTH.length;
            }
            file.putInt(position);
        }
        file.put(SOUTH).put(NORTH);
        file.putLong(0L).putInt(headerCrc ^ 2);
        data = file.array();
    }

    @Override
    public long length() {
        return data.length;
    }

    @Override
    public void readFully(long position, byte[] buffer, int offset, int length) {
        if (position >= HEADER_BYTES + INDEX_BYTES && position < data.length - 112) {
            cellReads++;
        }
        System.arraycopy(data, (int) position, buffer, offset, length);
    }

    void changeRevision(long timestamp) {
        ByteBuffer.wrap(data).putLong(data.length - 112, timestamp);
    }

    void corruptFirstCell() {
        data[HEADER_BYTES + INDEX_BYTES] ^= 1;
    }

    @Override
    public void close() {
        // In-memory fixture has no resources to close.
    }
}
