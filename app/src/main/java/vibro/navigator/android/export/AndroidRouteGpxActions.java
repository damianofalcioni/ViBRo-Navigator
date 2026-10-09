package vibro.navigator.android.export;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.android.storage.AndroidOutputFolderCleaner;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Serializes document-provider I/O away from navigation controls and their animations. */
public final class AndroidRouteGpxActions {
    public interface ClearResult {
        void onCleared(boolean cleared);
    }

    private static final String TAG = "GpxExport";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    private AndroidRouteGpxActions() {
    }

    public static void export(Activity activity, String gpx) {
        Context context = activity.getApplicationContext();
        WORKER.execute(() -> {
            try {
                AppLogger.dMultiline(TAG, "Generated route GPX XML", gpx);
                Intent chooser = AndroidRouteGpxViewIntent.createChooser(context, gpx);
                activity.runOnUiThread(() -> launch(activity, chooser));
            } catch (IOException | RuntimeException e) {
                reportFailure(context, e);
            }
        });
    }

    private static void launch(Activity activity, Intent chooser) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            activity.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            toast(activity, R.string.msg_route_export_no_app);
        } catch (RuntimeException e) {
            reportFailure(activity.getApplicationContext(), e);
        }
    }

    public static void autoSave(Context context, String gpx) {
        Context appContext = context.getApplicationContext();
        WORKER.execute(() -> {
            try {
                AppLogger.i(TAG, "Auto-saved route GPX uri="
                        + AndroidRouteGpxAutoSaver.saveUri(appContext, gpx));
            } catch (IOException | RuntimeException e) {
                reportFailure(appContext, e);
            }
        });
    }

    private static void reportFailure(Context context, Exception error) {
        AppLogger.w(TAG, "Could not save GPX to output folder", error);
        new Handler(Looper.getMainLooper()).post(() -> toast(context, R.string.msg_route_export_failed));
    }

    public static void clearFolder(Context context, String selected, ClearResult result) {
        Context appContext = context.getApplicationContext();
        WORKER.execute(() -> {
            boolean cleared;
            try {
                AndroidOutputFolderCleaner.clear(appContext, Kind.GPX, selected);
                cleared = true;
            } catch (IOException | RuntimeException e) {
                AppLogger.w(TAG, "Could not clear GPX output folder", e);
                cleared = false;
            }
            result.onCleared(cleared);
        });
    }

    private static void toast(Context context, int message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
