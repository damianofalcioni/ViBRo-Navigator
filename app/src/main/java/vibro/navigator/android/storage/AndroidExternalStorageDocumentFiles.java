package vibro.navigator.android.storage;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.logging.AppLogger;

final class AndroidExternalStorageDocumentFiles {
    private static final String TAG = "AndroidDocumentAccess";
    private static final String FILE_SCHEME = "file";

    private AndroidExternalStorageDocumentFiles() {
    }

    static boolean directoryExists(@NonNull Context context, @NonNull String documentId) {
        return readableDirectory(context, documentId) != null;
    }

    @Nullable
    static List<String> childDisplayNames(@NonNull Context context, @NonNull String documentId) {
        File directory = readableDirectory(context, documentId);
        if (directory == null) {
            return null;
        }
        String[] names = directory.list();
        if (names == null) {
            AppLogger.d(TAG, "Direct external storage directory listing unavailable documentId=" + documentId);
            return null;
        }
        List<String> out = new ArrayList<>();
        Collections.addAll(out, names);
        return out;
    }

    @Nullable
    static Uri childFileUri(
            @NonNull Context context,
            @NonNull String directoryDocumentId,
            @NonNull String displayName
    ) {
        File directory = readableDirectory(context, directoryDocumentId);
        if (directory == null || !isPlainDisplayName(displayName)) {
            return null;
        }
        return readableFileUri(new File(directory, displayName));
    }

    static boolean isFileUri(@NonNull Uri uri) {
        return FILE_SCHEME.equals(uri.getScheme());
    }

    @Nullable
    static File fileFromUri(@NonNull Uri uri) {
        if (!isFileUri(uri)) {
            return null;
        }
        String path = uri.getPath();
        return path == null || path.trim().isEmpty() ? null : new File(path);
    }

    @Nullable
    static String readText(@NonNull Uri uri) {
        File file = fileFromUri(uri);
        if (file == null || !file.isFile() || !file.canRead()) {
            return null;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            return readUtf8(input);
        } catch (IOException e) {
            AppLogger.w(TAG, "Failed to read direct external storage text uri=" + uri, e);
            return null;
        }
    }

    @Nullable
    private static File fileForDocumentId(@NonNull Context context, @NonNull String documentId) {
        int separator = documentId.indexOf(':');
        if (separator <= 0 || separator >= documentId.length() - 1) {
            return null;
        }
        String relativePath = documentId.substring(separator + 1);
        if (hasUnsafeRelativePath(relativePath)) {
            return null;
        }
        File root = AndroidStorageVolumes.storageRoot(context, documentId.substring(0, separator));
        return root == null ? null : new File(root, relativePath);
    }

    @Nullable
    private static File readableDirectory(@NonNull Context context, @NonNull String documentId) {
        File directory = fileForDocumentId(context, documentId);
        return directory != null && directory.isDirectory() && directory.canRead() ? directory : null;
    }

    @Nullable
    private static Uri readableFileUri(@NonNull File file) {
        return file.isFile() && file.canRead() ? Uri.fromFile(file) : null;
    }

    private static boolean isPlainDisplayName(@NonNull String displayName) {
        return displayName.indexOf('/') < 0 && displayName.indexOf('\\') < 0;
    }

    private static boolean hasUnsafeRelativePath(@NonNull String relativePath) {
        return relativePath.startsWith("/")
                || relativePath.startsWith("\\")
                || relativePath.contains("../")
                || relativePath.contains("..\\");
    }

    @NonNull
    private static String readUtf8(@NonNull FileInputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
