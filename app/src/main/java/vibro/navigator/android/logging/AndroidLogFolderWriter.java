package vibro.navigator.android.logging;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import vibro.navigator.android.storage.AndroidOutputFolderAccess;
import vibro.navigator.android.storage.AndroidWritableDocumentTree;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** A single selected-folder destination; no app-local mirror is created. */
public final class AndroidLogFolderWriter {
    private static final long MAX_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_BYTES = 3L * 1024L * 1024L;
    private final Context context;
    private final Uri document;
    private OutputStream stream;
    private long size;

    private AndroidLogFolderWriter(Context context, Uri document, OutputStream stream) {
        this.context = context;
        this.document = document;
        this.stream = stream;
    }

    @Nullable
    public static AndroidLogFolderWriter open(Context context, String name) {
        Uri document = null;
        try {
            Uri folder = AndroidOutputFolderAccess.ensureFolder(context, Kind.LOGS);
            if (folder == null) {
                return null;
            }
            document = AndroidWritableDocumentTree.createFile(context, folder, "text/plain", name);
            return new AndroidLogFolderWriter(context.getApplicationContext(), document,
                    AndroidWritableDocumentTree.open(context, document));
        } catch (IOException | RuntimeException e) {
            AndroidWritableDocumentTree.removeQuietly(context, document);
            AndroidOutputFolderAccess.markUnavailable(context, Kind.LOGS);
            return null;
        }
    }

    public boolean append(CharSequence block) {
        try {
            AndroidOutputFolderAccess.ensureFolder(context, Kind.LOGS);
            if (size > MAX_BYTES) {
                trim();
            }
            byte[] bytes = block.toString().getBytes(StandardCharsets.UTF_8);
            stream.write(bytes);
            stream.flush();
            size += bytes.length;
            AndroidOutputFolderAccess.markAvailable(Kind.LOGS);
            return true;
        } catch (IOException | RuntimeException e) {
            AndroidOutputFolderAccess.markUnavailable(context, Kind.LOGS);
            close();
            return false;
        }
    }

    private void trim() throws IOException {
        close();
        byte[] tail = readTail();
        stream = AndroidWritableDocumentTree.open(context, document);
        stream.write(tail);
        size = tail.length;
    }

    private byte[] readTail() throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(document)) {
            if (in == null) {
                throw new IOException("Log document is unavailable");
            }
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
        return document.toString();
    }

    public void close() {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            AndroidOutputFolderAccess.markUnavailable(context, Kind.LOGS);
        }
        stream = null;
    }
}
