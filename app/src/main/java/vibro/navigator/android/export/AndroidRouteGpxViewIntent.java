package vibro.navigator.android.export;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidFileActionChooser;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;

public final class AndroidRouteGpxViewIntent {
    private AndroidRouteGpxViewIntent() {
    }

    @NonNull
    public static Intent create(@NonNull Context context, @NonNull String gpx) throws IOException {
        return createForUri(context, AndroidRouteGpxAutoSaver.saveUri(context, gpx));
    }

    @NonNull
    public static Intent createChooser(@NonNull Context context, @NonNull String gpx) throws IOException {
        return createChooserForIntent(context, create(context, gpx));
    }

    @NonNull
    static Intent createChooserForIntent(@NonNull Context context, @NonNull Intent actionView) {
        return AndroidFileActionChooser.create(context, actionView,
                context.getString(R.string.action_export_route), context.getString(R.string.label_exported_route_gpx));
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
        return writeExportFile(context, gpx, new Date());
    }

    @NonNull
    static File writeExportFile(@NonNull Context context, @NonNull String gpx, @NonNull Date now) throws IOException {
        return AndroidRouteGpxAutoSaver.save(context.getApplicationContext(), gpx, now);
    }
}
