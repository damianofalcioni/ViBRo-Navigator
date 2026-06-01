package vibro.navigator.android.orientation;

import android.content.Context;
import android.os.Handler;

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
            @NonNull Handler uiHandler,
            @NonNull NavigationOrientationController.CompassUiState compassUiState
    ) {
        return new NavigationOrientationController(
                callback -> new AndroidGeomagneticOrientationMonitor(context, callback),
                new AndroidDisplayRotationProvider(context),
                AndroidElapsedRealtimeClock.INSTANCE,
                uiHandler::post,
                compassUiState
        );
    }
}
