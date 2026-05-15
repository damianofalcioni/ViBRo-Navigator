package vibro.navigator.nav.export;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import vibro.navigator.R;

public final class NavigationRouteGpxViewIntent {
    private static final String FILE_PROVIDER_SUFFIX = ".fileprovider";
    private static final String EXPORT_DIR = "exports";
    private static final String EXPORT_FILE_NAME = "current-route.gpx";

    private NavigationRouteGpxViewIntent() {
    }

    @NonNull
    public static Intent create(@NonNull Context context, @NonNull String gpx) throws IOException {
        File file = writeExportFile(context, gpx);
        Uri uri = FileProvider.getUriForFile(context, authority(context), file);
        return createForUri(context, uri);
    }

    @NonNull
    public static Intent createChooser(@NonNull Context context, @NonNull String gpx) throws IOException {
        return createChooserForIntent(context, create(context, gpx));
    }

    @NonNull
    static Intent createChooserForIntent(@NonNull Context context, @NonNull Intent actionView) {
        return Intent.createChooser(actionView, context.getString(R.string.action_export_route));
    }

    @NonNull
    static Intent createForUri(@NonNull Context context, @NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, NavigationRouteGpxExporter.GPX_MIME_TYPE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri(
                context.getString(R.string.label_exported_route_gpx),
                uri
        ));
        return intent;
    }

    @NonNull
    static File writeExportFile(@NonNull Context context, @NonNull String gpx) throws IOException {
        File exportDir = new File(context.getCacheDir(), EXPORT_DIR);
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            throw new IOException("Could not create route export cache directory");
        }
        File file = new File(exportDir, EXPORT_FILE_NAME);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file),
                StandardCharsets.UTF_8
        )) {
            writer.write(gpx);
        }
        return file;
    }

    @NonNull
    private static String authority(@NonNull Context context) {
        return context.getPackageName() + FILE_PROVIDER_SUFFIX;
    }
}
