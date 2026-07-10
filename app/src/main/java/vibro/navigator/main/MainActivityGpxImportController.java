package vibro.navigator.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.intent.GpxWaypointParser;
import vibro.navigator.intent.GpxWaypointRoute;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.ui.PoiInputController;

/** Handles external GPX content without blocking the main activity's UI thread. */
final class MainActivityGpxImportController {
    private static final String TAG = "MainGpxImport";
    private static final String GPX_MIME_TYPE = "application/gpx+xml";
    private static final String GPX_MIME_TYPE_LEGACY = "application/gpx";
    private static final String GPX_MIME_TYPE_ALTERNATE = "application/x-gpx+xml";
    @NonNull
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @NonNull
    private final Activity activity;
    @NonNull
    private final Intent incomingIntent;
    @NonNull
    private final PoiInputController destinationController;
    @NonNull
    private final MainActivityStopController stopController;
    @NonNull
    private final PoiHistoryStore historyStore;
    @NonNull
    private final MainActivityRouteModeController routeModeController;
    @NonNull
    private final TaskScheduler mainThreadScheduler;
    @NonNull
    private final GpxWaypointParser parser;

    private MainActivityGpxImportController(
            @NonNull Activity activity,
            @NonNull Intent incomingIntent,
            @NonNull PoiInputController destinationController,
            @NonNull MainActivityStopController stopController,
            @NonNull PoiHistoryStore historyStore,
            @NonNull MainActivityRouteModeController routeModeController
    ) {
        this.activity = activity;
        this.incomingIntent = incomingIntent;
        this.destinationController = destinationController;
        this.stopController = stopController;
        this.historyStore = historyStore;
        this.routeModeController = routeModeController;
        mainThreadScheduler = AndroidTaskScheduler.main();
        parser = new GpxWaypointParser();
    }

    static boolean importIfSupported(
            @NonNull Activity activity,
            @NonNull Intent intent,
            @NonNull PoiInputController destinationController,
            @NonNull MainActivityStopController stopController,
            @NonNull PoiHistoryStore historyStore,
            @NonNull MainActivityRouteModeController routeModeController
    ) {
        Uri uri = gpxUri(intent);
        if (uri == null) {
            return false;
        }
        new MainActivityGpxImportController(
                activity,
                intent,
                destinationController,
                stopController,
                historyStore,
                routeModeController
        ).startImport(uri);
        return true;
    }

    static boolean isGpxImportIntent(@Nullable Intent intent) {
        return intent != null && gpxUri(intent) != null;
    }

    @Nullable
    private static Uri gpxUri(@NonNull Intent intent) {
        if (!isSupportedGpxType(intent.getType()) && !hasGpxFileName(intent)) {
            return null;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            return intent.getData();
        }
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        }
        return null;
    }

    private static boolean isSupportedGpxType(@Nullable String mimeType) {
        return GPX_MIME_TYPE.equalsIgnoreCase(mimeType)
                || GPX_MIME_TYPE_LEGACY.equalsIgnoreCase(mimeType)
                || GPX_MIME_TYPE_ALTERNATE.equalsIgnoreCase(mimeType);
    }

    private static boolean hasGpxFileName(@NonNull Intent intent) {
        Uri uri = Intent.ACTION_SEND.equals(intent.getAction())
                ? IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class)
                : intent.getData();
        return uri != null && uri.getLastPathSegment() != null
                && uri.getLastPathSegment().toLowerCase(Locale.ROOT).endsWith(".gpx");
    }

    private void startImport(@NonNull Uri uri) {
        AppLogger.i(TAG, "Reading incoming GPX uri=" + uri);
        EXECUTOR.execute(() -> readRoute(uri));
    }

    private void readRoute(@NonNull Uri uri) {
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("No input stream for GPX document");
            }
            GpxWaypointRoute route = parser.parse(input);
            mainThreadScheduler.post(() -> applyRoute(route));
        } catch (IOException e) {
            AppLogger.w(TAG, "Could not read incoming GPX uri=" + uri, e);
            mainThreadScheduler.post(this::showImportFailure);
        }
    }

    private void applyRoute(@NonNull GpxWaypointRoute route) {
        if (!isCurrentIntent()) {
            return;
        }
        routeModeController.showRouteMode();
        destinationController.setPoi(route.destination);
        stopController.replaceStops(route.stops);
        rememberStops(route.stops);
        AppLogger.i(TAG, "Applied incoming GPX destination=" + route.destination.displayLabel()
                + " stopCount=" + route.stops.size());
    }

    private void rememberStops(@NonNull List<Poi> stops) {
        for (Poi stop : stops) {
            historyStore.addOrPromote(stop);
        }
    }

    private void showImportFailure() {
        if (!isCurrentIntent()) {
            return;
        }
        Toast.makeText(activity, R.string.msg_gpx_import_failed, Toast.LENGTH_SHORT).show();
    }

    private boolean isCurrentIntent() {
        return activity.getIntent() == incomingIntent
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }
}
