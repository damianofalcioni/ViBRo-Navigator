package vibro.navigator.nav.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import vibro.navigator.android.export.AndroidRouteGpxActions;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;

final class NavigationStopGpxAutoSave {
    private static final String TAG = "NavigationActivity";

    interface RouteGpxSource {
        @Nullable
        String buildCurrentRouteGpx();
    }

    interface RouteGpxSaver {
        @NonNull
        File save(@NonNull String gpx) throws IOException;
    }

    private NavigationStopGpxAutoSave() {
    }

    static void saveIfEnabled(@NonNull Context context, @NonNull RouteGpxSource source) {
        String gpx = buildIfEnabled(AppGpxSettings.isAutoSaveOnStopEnabled(context), source);
        if (gpx != null) {
            AndroidRouteGpxActions.autoSave(context, gpx);
        }
    }

    @Nullable
    static File saveIfEnabled(
            boolean enabled,
            @NonNull RouteGpxSource source,
            @NonNull RouteGpxSaver saver
    ) {
        String gpx = buildIfEnabled(enabled, source);
        if (gpx == null) {
            return null;
        }
        try {
            File file = saver.save(gpx);
            AppLogger.i(TAG, "Auto-saved route GPX path=" + file.getAbsolutePath());
            return file;
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to auto-save route GPX on stop", e);
            return null;
        }
    }

    @Nullable
    private static String buildIfEnabled(boolean enabled, @NonNull RouteGpxSource source) {
        if (!enabled) {
            return null;
        }
        String gpx = source.buildCurrentRouteGpx();
        if (gpx == null) {
            AppLogger.w(TAG, "Auto-save GPX skipped because no active route is available");
        }
        return gpx;
    }
}
