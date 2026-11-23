package vibro.navigator.nav.service;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterStreetGrantChanges;
import vibro.navigator.android.brouter.AndroidBRouterVolumeChanges;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.brouter.BRouterSegmentSourceSettings;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.streets.SurroundingStreetOverlayController;

final class NavigationServiceStreetOverlay {
    @NonNull
    private final SurroundingStreetOverlayController controller;
    @NonNull
    private final Runnable stopObservingSources;
    private final AndroidBRouterStreetGrantChanges streetGrantChanges;
    private boolean viewportOpen;

    private NavigationServiceStreetOverlay(@NonNull Context context, @NonNull SurroundingStreetOverlayController controller) {
        this.controller = controller;
        streetGrantChanges = new AndroidBRouterStreetGrantChanges(context, AndroidElapsedRealtimeClock.INSTANCE,
                controller::onSourcesChanged);
        Runnable stopSettingsObserver = BRouterSegmentSourceSettings.observe(context, () -> {
            streetGrantChanges.reset();
            if (viewportOpen) {
                streetGrantChanges.check();
            }
            controller.onSourcesChanged();
        });
        Runnable stopVolumeObserver = AndroidBRouterVolumeChanges.observe(context, controller::onSourcesChanged);
        stopObservingSources = () -> {
            stopSettingsObserver.run();
            stopVolumeObserver.run();
        };
    }

    @NonNull
    static NavigationServiceStreetOverlay create(
            @NonNull Context context,
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable stateEmitter
    ) {
        return new NavigationServiceStreetOverlay(context, new SurroundingStreetOverlayController(
                context,
                scheduler,
                AndroidElapsedRealtimeClock.INSTANCE,
                AndroidBRouterSegmentsRepositoryFactory::create,
                stateEmitter
        ));
    }

    void reset() {
        viewportOpen = false;
        streetGrantChanges.reset();
        controller.reset();
    }

    void onAcceptedLocation(@NonNull NavigationLocation location) {
        if (viewportOpen) {
            streetGrantChanges.check();
        }
        controller.onAcceptedLocation(location);
    }

    void onCompassViewport(@NonNull NavCompassState compassState) {
        viewportOpen = true;
        streetGrantChanges.check();
        controller.onCompassViewport(compassState);
    }

    void clearCompassViewport() {
        viewportOpen = false;
        streetGrantChanges.checkOnNextViewport();
        controller.clearCompassViewport();
    }

    @NonNull
    NavState attach(@NonNull NavState state) {
        return NavStateComposer.withCompassStreetOverlay(state, controller.currentOverlay());
    }

    void shutdown() {
        stopObservingSources.run();
        controller.shutdown();
    }
}
