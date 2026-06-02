package vibro.navigator.android.export;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidIntentCompat;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;

public final class AndroidRouteGpxViewIntent {
    private static final String FILE_PROVIDER_SUFFIX = ".fileprovider";
    private static final String EXPORT_DIR = "exports";
    private static final String EXPORT_FILE_NAME = "current-route.gpx";

    private AndroidRouteGpxViewIntent() {
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
        Intent send = createSendIntent(context, actionView);
        List<Intent> targets = explicitTargets(context, actionView);
        Intent chooser = Intent.createChooser(send, context.getString(R.string.action_export_route));
        if (!targets.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.toArray(new Parcelable[0]));
        }
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooser.setClipData(send.getClipData());
        AndroidIntentCompat.disableAutoLaunchSingleChoice(chooser);
        return chooser;
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

    @NonNull
    private static Intent createSendIntent(@NonNull Context context, @NonNull Intent actionView) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(actionView.getType());
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(actionView.getClipData());
        if (actionView.getData() != null) {
            send.putExtra(Intent.EXTRA_STREAM, actionView.getData());
        }
        send.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.label_exported_route_gpx));
        return send;
    }

    @NonNull
    private static List<Intent> explicitTargets(@NonNull Context context, @NonNull Intent actionView) {
        return AndroidIntentCompat.explicitActivityTargets(context, actionView);
    }
}
