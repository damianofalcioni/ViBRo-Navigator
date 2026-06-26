package vibro.navigator.nav.service;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.streets.SurroundingStreetOverlayController;

final class NavigationServiceStreetOverlay {
    @NonNull
    private final SurroundingStreetOverlayController controller;

    private NavigationServiceStreetOverlay(@NonNull SurroundingStreetOverlayController controller) {
        this.controller = controller;
    }

    @NonNull
    static NavigationServiceStreetOverlay create(
            @NonNull Context context,
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable stateEmitter
    ) {
        return new NavigationServiceStreetOverlay(new SurroundingStreetOverlayController(
                context,
                scheduler,
                AndroidElapsedRealtimeClock.INSTANCE,
                AndroidBRouterSegmentsRepositoryFactory.create(),
                stateEmitter
        ));
    }

    void reset() {
        controller.reset();
    }

    void onAcceptedLocation(@NonNull NavigationLocation location) {
        controller.onAcceptedLocation(location);
    }

    void onCompassViewport(@NonNull NavCompassState compassState) {
        controller.onCompassViewport(compassState);
    }

    void clearCompassViewport() {
        controller.clearCompassViewport();
    }

    @NonNull
    NavState attach(@NonNull NavState state) {
        return NavStateComposer.withCompassStreetOverlay(state, controller.currentOverlay());
    }

    void shutdown() {
        controller.shutdown();
    }
}
