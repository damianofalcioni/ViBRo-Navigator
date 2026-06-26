package vibro.navigator.brouter;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

public interface BRouterSegmentReadFile extends Closeable {
    long length() throws IOException;

    void readFully(long position, @NonNull byte[] buffer, int offset, int length) throws IOException;

    @Override
    void close() throws IOException;
}
