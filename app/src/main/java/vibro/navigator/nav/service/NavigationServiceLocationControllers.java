package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.orientation.NavigationOrientationController;
import vibro.navigator.nav.foreground.NavigationForegroundController;

/** Runtime-owned controllers attached together before fixes can be evaluated. */
final class NavigationServiceLocationControllers {
    @NonNull
    public final NavigationLocationController locationController;
    @NonNull
    public final NavigationOrientationController orientationController;
    @NonNull
    public final NavigationForegroundController foregroundController;

    NavigationServiceLocationControllers(
            @NonNull NavigationLocationController locationController,
            @NonNull NavigationOrientationController orientationController,
            @NonNull NavigationForegroundController foregroundController
    ) {
        this.locationController = locationController;
        this.orientationController = orientationController;
        this.foregroundController = foregroundController;
    }
}
