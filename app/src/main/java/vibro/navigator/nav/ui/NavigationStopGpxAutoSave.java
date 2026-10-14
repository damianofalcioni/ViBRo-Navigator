package vibro.navigator.nav.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.export.AndroidRouteGpxActions;
import vibro.navigator.nav.export.NavigationRouteGpxSource;
import vibro.navigator.settings.AppGpxSettings;

final class NavigationStopGpxAutoSave {
    interface RouteGpxSource {
        @Nullable
        NavigationRouteGpxSource captureCurrentRouteGpx();
    }

    interface ExportQueue {
        void enqueue(@NonNull NavigationRouteGpxSource source);
    }

    private NavigationStopGpxAutoSave() {
    }

    static void saveIfEnabled(@NonNull Context context, @NonNull RouteGpxSource source) {
        Context appContext = context.getApplicationContext();
        saveIfEnabled(AppGpxSettings.isAutoSaveOnStopRequested(appContext), source,
                captured -> AndroidRouteGpxActions.autoSave(appContext, captured));
    }

    static void saveIfEnabled(
            boolean enabled,
            @NonNull RouteGpxSource source,
            @NonNull ExportQueue queue
    ) {
        if (!enabled) {
            return;
        }
        NavigationRouteGpxSource captured = source.captureCurrentRouteGpx();
        if (captured != null) {
            queue.enqueue(captured);
        }
    }
}
