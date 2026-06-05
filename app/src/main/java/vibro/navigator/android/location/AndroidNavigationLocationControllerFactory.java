package vibro.navigator.android.location;

import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.location.FusedLocationUpdateClient;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.settings.AppSettings;

public final class AndroidNavigationLocationControllerFactory {
    private AndroidNavigationLocationControllerFactory() {
    }

    @NonNull
    public static NavigationLocationController create(
            @NonNull Context context,
            @NonNull NavigationLocationListener listener
    ) {
        Context appContext = context.getApplicationContext();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        AndroidLocationProviderAccess providerAccess = new AndroidLocationProviderAccess(
                context,
                locationManager,
                listener
        );
        FusedLocationUpdateClient fusedClient = DistributionServices.createFusedLocationUpdateClient(
                context,
                listener
        );
        return new NavigationLocationController(
                providerAccess,
                new AndroidGnssStatusTracker(locationManager),
                fusedClient,
                () -> DistributionServices.supportsFusedLocation()
                        && AppSettings.isFusedLocationEnabled(appContext)
                        && fusedClient.isAvailable(),
                AndroidElapsedRealtimeClock.INSTANCE
        );
    }
}
