package vibro.navigator.android.storage;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import vibro.navigator.brouter.BRouterSegmentReadFile;
import vibro.navigator.logging.AppLogger;

public final class AndroidDocumentReadFile {
    private static final String TAG = "AndroidDocumentReadFile";

    private AndroidDocumentReadFile() {
    }

    @Nullable
    public static BRouterSegmentReadFile open(@NonNull Context context, @NonNull Uri documentUri)
            throws IOException {
        File file = AndroidExternalStorageDocumentFiles.fileFromUri(documentUri);
        if (file != null) {
            return file.isFile() && file.canRead() ? AndroidFileReadFile.open(file) : null;
        }
        ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(documentUri, "r");
        if (descriptor == null) {
            AppLogger.w(TAG, "Document file descriptor unavailable uri=" + documentUri);
            return null;
        }
        return new ChannelReadFile(descriptor);
    }

    private static final class ChannelReadFile implements BRouterSegmentReadFile {
        @NonNull
        private final ParcelFileDescriptor descriptor;
        @NonNull
        private final FileInputStream inputStream;
        @NonNull
        private final FileChannel channel;

        ChannelReadFile(@NonNull ParcelFileDescriptor descriptor) {
            this.descriptor = descriptor;
            this.inputStream = new FileInputStream(descriptor.getFileDescriptor());
            this.channel = inputStream.getChannel();
        }

        @Override
        public long length() throws IOException {
            long statSize = descriptor.getStatSize();
            return statSize >= 0L ? statSize : channel.size();
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
                    throw new IOException("Unexpected end of document file");
                }
            }
        }

        @Override
        public void close() throws IOException {
            try {
                inputStream.close();
            } finally {
                descriptor.close();
            }
        }
    }
}
