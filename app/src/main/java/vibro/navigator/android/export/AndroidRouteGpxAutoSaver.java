package vibro.navigator.android.export;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.AndroidOutputFile;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.settings.AppGpxSettings;

public final class AndroidRouteGpxAutoSaver {
    private static final String FILE_PREFIX = "vibro-navigator-route-";
    private static final String FILE_SUFFIX = ".gpx";

    private AndroidRouteGpxAutoSaver() {
    }

    @NonNull
    public static Uri saveUri(@NonNull Context context, @NonNull String gpx) throws IOException {
        AndroidOutputFile file = null;
        try {
            file = AndroidOutputStorage.create(context, Kind.GPX,
                    NavigationRouteGpxExporter.GPX_MIME_TYPE, buildFileName(new Date()));
            write(file, gpx);
            return file.uri;
        } catch (IOException | RuntimeException e) {
            AppGpxSettings.setAutoSaveOnStopEnabled(context, false);
            if (file != null) {
                file.removeQuietly();
                AndroidOutputStorage.failed(Kind.GPX, file.destination);
            }
            throw new IOException("Could not save GPX to selected output folder", e);
        }
    }

    private static void write(AndroidOutputFile file, String gpx) throws IOException {
        try (OutputStream out = file.open()) {
            out.write(gpx.getBytes(StandardCharsets.UTF_8));
        }
        file.publish();
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
        File dir = AndroidOutputStorage.requested(context, Kind.GPX).directory;
        if (dir == null) {
            throw new IOException("Selected GPX storage requires a document URI");
        }
        if (dir.isDirectory() || dir.mkdirs() || dir.isDirectory()) {
            return dir;
        }
        throw new IOException("Could not create route GPX directory");
    }

}
