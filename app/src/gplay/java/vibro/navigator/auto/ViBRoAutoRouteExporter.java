package vibro.navigator.auto;

import android.content.ActivityNotFoundException;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

import java.io.IOException;

import vibro.navigator.R;
import vibro.navigator.android.export.AndroidRouteGpxViewIntent;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class ViBRoAutoRouteExporter {
    private static final String TAG = "ViBRoCarScreen";

    interface Host {
        void showToast(int messageResId);
    }

    private ViBRoAutoRouteExporter() {
    }

    static void exportCurrentRoute(
            @NonNull CarContext carContext,
            @Nullable NavigationServiceBinder navBinder,
            @NonNull Host host
    ) {
        if (navBinder == null) {
            AppLogger.w(TAG, "Route export requested before service binding completed");
            host.showToast(R.string.msg_route_export_unavailable);
            return;
        }
        String gpx = navBinder.buildCurrentRouteGpx();
        if (gpx == null) {
            AppLogger.w(TAG, "Route export requested without an active route");
            host.showToast(R.string.msg_route_export_unavailable);
            return;
        }
        launchChooser(carContext, gpx, host);
    }

    private static void launchChooser(
            @NonNull CarContext carContext,
            @NonNull String gpx,
            @NonNull Host host
    ) {
        AppLogger.dMultiline(TAG, "Generated route GPX XML from Android Auto", gpx);
        try {
            Intent chooser = AndroidRouteGpxViewIntent.createChooser(carContext, gpx)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            carContext.startActivity(chooser);
            AppLogger.i(TAG, "Route GPX chooser launched from Android Auto");
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "No app can open exported GPX route from Android Auto", e);
            host.showToast(R.string.msg_route_export_no_app);
        } catch (IOException | RuntimeException e) {
            AppLogger.w(TAG, "Failed to export current route as GPX from Android Auto", e);
            host.showToast(R.string.msg_route_export_failed);
        }
    }
}
