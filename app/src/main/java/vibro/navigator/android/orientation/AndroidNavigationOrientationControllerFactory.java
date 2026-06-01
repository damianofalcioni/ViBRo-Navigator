package vibro.navigator.android.orientation;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.android.display.AndroidDisplayRotationProvider;
import vibro.navigator.android.sensor.AndroidGeomagneticOrientationMonitor;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.orientation.NavigationOrientationController;

public final class AndroidNavigationOrientationControllerFactory {
    private AndroidNavigationOrientationControllerFactory() {
    }

    @NonNull
    public static NavigationOrientationController create(
            @NonNull Context context,
            @NonNull NavigationOrientationController.UiDispatcher uiDispatcher,
            @NonNull NavigationOrientationController.CompassUiState compassUiState
    ) {
        return new NavigationOrientationController(
                callback -> new AndroidGeomagneticOrientationMonitor(context, callback),
                new AndroidDisplayRotationProvider(context),
                AndroidElapsedRealtimeClock.INSTANCE,
                uiDispatcher,
                compassUiState
        );
    }
}
