package vibro.navigator.android.export;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vibro.navigator.android.storage.AndroidAppStorageDirs;

final class AndroidRouteGpxProviderRoots {
    private static final String ROOT_INTERNAL = "internal";
    private static final String ROOT_EXTERNAL_PREFIX = "external";

    private AndroidRouteGpxProviderRoots() {
    }

    @NonNull
    static Root rootForFile(@NonNull Context context, @NonNull File file) throws IOException {
        Root best = null;
        for (Root root : roots(context)) {
            if (root.contains(file) && isBetterRoot(root, best)) {
                best = root;
            }
        }
        if (best == null) {
            throw new IOException("Route GPX file is outside app storage");
        }
        return best;
    }

    @NonNull
    static Root rootByName(@NonNull Context context, @NonNull String name) throws IOException {
        for (Root root : roots(context)) {
            if (root.name.equals(name)) {
                return root;
            }
        }
        throw new FileNotFoundException("Unknown route GPX root");
    }

    private static boolean isBetterRoot(@NonNull Root candidate, @Nullable Root current) {
        return current == null || candidate.canonicalPath.length() > current.canonicalPath.length();
    }

    @NonNull
    private static List<Root> roots(@NonNull Context context) throws IOException {
        List<Root> roots = new ArrayList<>();
        roots.add(new Root(ROOT_INTERNAL, AndroidAppStorageDirs.internalFilesDir(context)));
        addExternalRoots(context, roots);
        return roots;
    }

    private static void addExternalRoots(@NonNull Context context, @NonNull List<Root> roots) throws IOException {
        File[] externalDirs = context.getExternalFilesDirs(null);
        if (externalDirs == null || externalDirs.length == 0) {
            addExternalRoot(roots, 0, context.getExternalFilesDir(null));
            return;
        }
        for (int i = 0; i < externalDirs.length; i++) {
            addExternalRoot(roots, i, externalDirs[i]);
        }
    }

    private static void addExternalRoot(
            @NonNull List<Root> roots,
            int index,
            @Nullable File dir
    ) throws IOException {
        if (dir != null) {
            roots.add(new Root(ROOT_EXTERNAL_PREFIX + index, dir));
        }
    }

    static final class Root {
        final String name;
        final File file;
        final String canonicalPath;

        private Root(@NonNull String name, @NonNull File file) throws IOException {
            this.name = name;
            this.file = file.getCanonicalFile();
            this.canonicalPath = this.file.getPath();
        }

        boolean contains(@NonNull File candidate) {
            String filePath = candidate.getPath();
            return filePath.equals(canonicalPath) || filePath.startsWith(canonicalPath + File.separator);
        }
    }
}
