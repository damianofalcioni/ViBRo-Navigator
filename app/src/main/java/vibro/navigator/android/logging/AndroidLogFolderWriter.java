package vibro.navigator.android.logging;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.AndroidOutputFile;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** A single selected Downloads destination; no fallback or app-local mirror is created. */
public final class AndroidLogFolderWriter {
    private static final long MAX_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_BYTES = 3L * 1024L * 1024L;
    private final Context context;
    private final AndroidOutputFile file;
    private OutputStream stream;
    private long size;
    private boolean published;

    private AndroidLogFolderWriter(Context context, AndroidOutputFile file, OutputStream stream) {
        this.context = context;
        this.file = file;
        this.stream = stream;
    }

    @Nullable
    public static AndroidLogFolderWriter open(Context context, String name) {
        AndroidOutputFile file = null;
        try {
            file = AndroidOutputStorage.create(context, Kind.LOGS, "text/plain", name);
            return new AndroidLogFolderWriter(context.getApplicationContext(), file, file.open());
        } catch (IOException | RuntimeException e) {
            if (file != null) {
                file.removeQuietly();
                AndroidOutputStorage.failed(Kind.LOGS, file.destination);
            }
            return null;
        }
    }

    public boolean append(CharSequence block) {
        try {
            if (!file.destination.token().equals(AndroidOutputStorage.requested(context, Kind.LOGS).token())) {
                close();
                return false;
            }
            if (size > MAX_BYTES) {
                trim();
            }
            byte[] bytes = block.toString().getBytes(StandardCharsets.UTF_8);
            stream.write(bytes);
            stream.flush();
            size += bytes.length;
            publish();
            return true;
        } catch (IOException | RuntimeException e) {
            AndroidOutputStorage.failed(Kind.LOGS, file.destination);
            if (!published) {
                file.removeQuietly();
            }
            close();
            return false;
        }
    }

    private void publish() throws IOException {
        if (!published) {
            file.publish();
            published = true;
        }
    }

    private void trim() throws IOException {
        close();
        byte[] tail = readTail();
        stream = file.open();
        stream.write(tail);
        size = tail.length;
    }

    private byte[] readTail() throws IOException {
        try (InputStream in = file.read()) {
            skip(in, size - KEEP_BYTES);
            int value;
            do {
                value = in.read();
            } while (value != -1 && value != '\n');
            ByteArrayOutputStream tail = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                tail.write(buffer, 0, count);
            }
            return tail.toByteArray();
        }
    }

    private static void skip(InputStream in, long remaining) throws IOException {
        long bytesLeft = remaining;
        while (bytesLeft > 0) {
            long skipped = in.skip(bytesLeft);
            if (skipped == 0) {
                if (in.read() == -1) {
                    return;
                }
                skipped = 1;
            }
            bytesLeft -= skipped;
        }
    }

    public String path() {
        return file.path();
    }

    public void close() {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            AndroidOutputStorage.failed(Kind.LOGS, file.destination);
        }
        stream = null;
    }
}
