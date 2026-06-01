package vibro.navigator.nav.service;

import android.os.Handler;

import androidx.annotation.NonNull;

import vibro.navigator.android.location.AndroidNavigationLocationControllerFactory;
import vibro.navigator.android.orientation.AndroidNavigationOrientationControllerFactory;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.orientation.NavigationOrientationController;

final class NavigationTrackingRuntime {
    @NonNull
    final NavigationLocationController locationController;
    @NonNull
    final NavigationOrientationController orientationController;

    private NavigationTrackingRuntime(
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationOrientationController orientationController
    ) {
        this.locationController = locationController;
        this.orientationController = orientationController;
    }

    @NonNull
    static NavigationTrackingRuntime create(
            @NonNull NavigationService service,
            @NonNull Handler handler,
            @NonNull NavigationServiceUiVisibility uiVisibility,
            @NonNull NavigationServiceLocationHandler locationHandler,
            @NonNull NavigationForegroundController foregroundController
    ) {
        NavigationLocationController locationController = AndroidNavigationLocationControllerFactory.create(
                service,
                locationHandler
        );
        NavigationOrientationController orientationController = AndroidNavigationOrientationControllerFactory.create(
                service,
                handler,
                uiVisibility
        );
        locationHandler.attachControllers(locationController, orientationController, foregroundController);
        return new NavigationTrackingRuntime(locationController, orientationController);
    }
}
