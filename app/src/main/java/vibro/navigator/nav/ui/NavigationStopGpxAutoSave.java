package vibro.navigator.nav.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import vibro.navigator.android.export.AndroidRouteGpxAutoSaver;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;

final class NavigationStopGpxAutoSave {
    private static final String TAG = "NavigationActivity";

    interface RouteGpxSource {
        @Nullable
        String buildCurrentRouteGpx();
    }

    private NavigationStopGpxAutoSave() {
    }

    @Nullable
    static File saveIfEnabled(@NonNull Context context, @NonNull RouteGpxSource source) {
        if (!AppGpxSettings.isAutoSaveOnStopEnabled(context)) {
            return null;
        }
        String gpx = source.buildCurrentRouteGpx();
        if (gpx == null) {
            AppLogger.w(TAG, "Auto-save GPX skipped because no active route is available");
            return null;
        }
        try {
            File file = AndroidRouteGpxAutoSaver.save(context, gpx);
            AppLogger.i(TAG, "Auto-saved route GPX path=" + file.getAbsolutePath());
            return file;
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to auto-save route GPX on stop", e);
            return null;
        }
    }
}
