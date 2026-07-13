package vibro.navigator.android.export;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;

public final class AndroidRouteGpxViewIntent {
    private AndroidRouteGpxViewIntent() {
    }

    @NonNull
    public static Intent create(@NonNull Context context, @NonNull String gpx) throws IOException {
        File file = writeExportFile(context, gpx);
        Uri uri = AndroidRouteGpxFileProvider.uriForFile(context, file);
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
        disableAutoLaunchSingleChoice(chooser);
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
        return writeExportFile(context, gpx, new Date());
    }

    @NonNull
    static File writeExportFile(@NonNull Context context, @NonNull String gpx, @NonNull Date now) throws IOException {
        return AndroidRouteGpxAutoSaver.save(context.getApplicationContext(), gpx, now);
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
        return explicitActivityTargets(context, actionView);
    }

    private static void disableAutoLaunchSingleChoice(@NonNull Intent chooser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            chooser.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
        }
    }

    @NonNull
    private static List<Intent> explicitActivityTargets(@NonNull Context context, @NonNull Intent actionView) {
        List<ResolveInfo> handlers = queryViewHandlers(context, actionView);
        List<Intent> targets = new ArrayList<>(handlers.size());
        for (ResolveInfo handler : handlers) {
            ActivityInfo activityInfo = handler.activityInfo;
            if (activityInfo == null || activityInfo.packageName == null || activityInfo.name == null) {
                continue;
            }
            if (context.getPackageName().equals(activityInfo.packageName)) {
                continue;
            }
            targets.add(new Intent(actionView).setComponent(new ComponentName(
                    activityInfo.packageName,
                    activityInfo.name
            )));
        }
        return targets;
    }

    @NonNull
    private static List<ResolveInfo> queryViewHandlers(@NonNull Context context, @NonNull Intent actionView) {
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.queryIntentActivities(
                    actionView,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY)
            );
        }
        return packageManager.queryIntentActivities(actionView, PackageManager.MATCH_DEFAULT_ONLY);
    }
}
