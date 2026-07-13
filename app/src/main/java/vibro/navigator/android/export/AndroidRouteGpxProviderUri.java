package vibro.navigator.android.export;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

final class AndroidRouteGpxProviderUri {
    private AndroidRouteGpxProviderUri() {
    }

    @NonNull
    static Uri uriForFile(
            @NonNull Context context,
            @NonNull File file,
            @NonNull String authority
    ) throws IOException {
        File canonicalFile = file.getCanonicalFile();
        AndroidRouteGpxProviderRoots.Root root =
                AndroidRouteGpxProviderRoots.rootForFile(context, canonicalFile);
        Uri.Builder builder = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(root.name);
        appendRelativePath(builder, root, canonicalFile);
        return builder.build();
    }

    @NonNull
    static File fileForUri(@NonNull Context context, @NonNull Uri uri) throws FileNotFoundException {
        List<String> segments = uri.getPathSegments();
        if (segments.size() < 2) {
            throw new FileNotFoundException("Route GPX URI has no file path");
        }
        return resolveFile(context, segments);
    }

    @NonNull
    private static File resolveFile(
            @NonNull Context context,
            @NonNull List<String> segments
    ) throws FileNotFoundException {
        try {
            AndroidRouteGpxProviderRoots.Root root =
                    AndroidRouteGpxProviderRoots.rootByName(context, segments.get(0));
            File file = fileInRoot(root, segments);
            if (!root.contains(file)) {
                throw new FileNotFoundException("Route GPX URI escapes app storage");
            }
            return file;
        } catch (IOException e) {
            FileNotFoundException notFound = new FileNotFoundException("Could not resolve route GPX URI");
            notFound.initCause(e);
            throw notFound;
        }
    }

    @NonNull
    private static File fileInRoot(
            @NonNull AndroidRouteGpxProviderRoots.Root root,
            @NonNull List<String> segments
    ) throws IOException {
        File file = root.file;
        for (int i = 1; i < segments.size(); i++) {
            file = new File(file, segments.get(i));
        }
        return file.getCanonicalFile();
    }

    private static void appendRelativePath(
            @NonNull Uri.Builder builder,
            @NonNull AndroidRouteGpxProviderRoots.Root root,
            @NonNull File file
    ) {
        String relativePath = file.getPath().substring(root.canonicalPath.length() + 1);
        String normalizedPath = relativePath.replace(File.separatorChar, '/');
        for (String segment : normalizedPath.split("/")) {
            builder.appendPath(segment);
        }
    }
}
