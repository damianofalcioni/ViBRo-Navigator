package vibro.navigator.android.storage;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import vibro.navigator.brouter.BRouterSegmentReadFile;

public final class AndroidFileReadFile {
    private AndroidFileReadFile() {
    }

    @NonNull
    public static BRouterSegmentReadFile open(@NonNull File file) throws IOException {
        return new ChannelReadFile(file);
    }

    private static final class ChannelReadFile implements BRouterSegmentReadFile {
        @NonNull
        private final FileInputStream inputStream;
        @NonNull
        private final FileChannel channel;

        ChannelReadFile(@NonNull File file) throws IOException {
            inputStream = new FileInputStream(file);
            channel = inputStream.getChannel();
        }

        @Override
        public long length() throws IOException {
            return channel.size();
        }

        @Override
        public synchronized void readFully(
                long position,
                @NonNull byte[] buffer,
                int offset,
                int length
        ) throws IOException {
            ByteBuffer target = ByteBuffer.wrap(buffer, offset, length);
            channel.position(position);
            while (target.hasRemaining()) {
                if (channel.read(target) < 0) {
                    throw new IOException("Unexpected end of file");
                }
            }
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}
