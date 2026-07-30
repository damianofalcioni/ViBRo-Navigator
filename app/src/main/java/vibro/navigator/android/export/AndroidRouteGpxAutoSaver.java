package vibro.navigator.android.export;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vibro.navigator.android.storage.AndroidAppStorageDirs;

public final class AndroidRouteGpxAutoSaver {
    private static final String GPX_DIR = "gpx";
    private static final String FILE_PREFIX = "vibro-navigator-route-";
    private static final String FILE_SUFFIX = ".gpx";

    private AndroidRouteGpxAutoSaver() {
    }

    @NonNull
    public static File save(@NonNull Context context, @NonNull String gpx) throws IOException {
        return save(context.getApplicationContext(), gpx, new Date());
    }

    @NonNull
    static File save(@NonNull Context context, @NonNull String gpx, @NonNull Date now) throws IOException {
        return saveToDirectory(ensureGpxDir(context), gpx, now);
    }

    @NonNull
    static File saveToDirectory(@NonNull File dir, @NonNull String gpx, @NonNull Date now) throws IOException {
        File file = nextFile(dir, now);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file),
                StandardCharsets.UTF_8
        )) {
            writer.write(gpx);
        }
        return file;
    }

    @NonNull
    static String buildFileName(@NonNull Date now) {
        return buildFileName(now, 1);
    }

    @NonNull
    static String buildFileName(@NonNull Date now, int collisionIndex) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now);
        String collisionSuffix = collisionIndex > 1 ? "-" + collisionIndex : "";
        return FILE_PREFIX + timestamp + collisionSuffix + FILE_SUFFIX;
    }

    @NonNull
    private static File nextFile(@NonNull File dir, @NonNull Date now) {
        int collisionIndex = 1;
        File file;
        do {
            file = new File(dir, buildFileName(now, collisionIndex));
            collisionIndex++;
        } while (file.exists());
        return file;
    }

    @NonNull
    private static File ensureGpxDir(@NonNull Context context) throws IOException {
        File dir = new File(resolveFilesRoot(context), GPX_DIR);
        if (dir.isDirectory() || dir.mkdirs() || dir.isDirectory()) {
            return dir;
        }
        throw new IOException("Could not create route GPX directory");
    }

    @NonNull
    private static File resolveFilesRoot(@NonNull Context context) {
        File externalBase = AndroidAppStorageDirs.preferredExternalFilesDir(context);
        return externalBase == null ? AndroidAppStorageDirs.internalFilesDir(context) : externalBase;
    }
}
