package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationProviders;

final class AndroidCurrentLocationSeeder {

    private static final String TAG = "NavLocation";

    @Nullable
    private final LocationManager locationManager;
    private final LocationListener listener;
    private final Executor locationCallbackExecutor;

    @Nullable
    private CancellationSignal gpsCurrentLocationCancellation;
    @Nullable
    private CancellationSignal networkCurrentLocationCancellation;

    AndroidCurrentLocationSeeder(
            @Nullable LocationManager locationManager,
            @NonNull LocationListener listener,
            @NonNull Executor locationCallbackExecutor
    ) {
        this.locationManager = locationManager;
        this.listener = listener;
        this.locationCallbackExecutor = locationCallbackExecutor;
    }

    void requestSeeds(boolean fineGranted, boolean coarseGranted) {
        if (!isCurrentLocationSeedSupported()) {
            return;
        }
        if (fineGranted && isProviderEnabled(NavigationLocationProviders.GPS_PROVIDER)) {
            gpsCurrentLocationCancellation =
                    requestCurrentLocationSeed(NavigationLocationProviders.GPS_PROVIDER);
        }
        if ((fineGranted || coarseGranted)
                && isProviderEnabled(NavigationLocationProviders.NETWORK_PROVIDER)) {
            networkCurrentLocationCancellation =
                    requestCurrentLocationSeed(NavigationLocationProviders.NETWORK_PROVIDER);
        }
    }

    void requestSeedForEnabledProvider(@NonNull String provider) {
        if (!isCurrentLocationSeedSupported()) {
            return;
        }
        if (NavigationLocationProviders.GPS_PROVIDER.equals(provider)) {
            gpsCurrentLocationCancellation = requestCurrentLocationSeed(provider);
        } else if (NavigationLocationProviders.NETWORK_PROVIDER.equals(provider)) {
            networkCurrentLocationCancellation = requestCurrentLocationSeed(provider);
        }
    }

    void cancelPendingCurrentLocationRequests() {
        if (gpsCurrentLocationCancellation != null) {
            gpsCurrentLocationCancellation.cancel();
            gpsCurrentLocationCancellation = null;
        }
        if (networkCurrentLocationCancellation != null) {
            networkCurrentLocationCancellation.cancel();
            networkCurrentLocationCancellation = null;
        }
    }

    private boolean isCurrentLocationSeedSupported() {
        return locationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    private boolean isProviderEnabled(@NonNull String provider) {
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read provider state provider=" + provider, e);
            return false;
        }
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private CancellationSignal requestCurrentLocationSeed(@NonNull String provider) {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        CancellationSignal cancellationSignal = new CancellationSignal();
        Consumer<Location> consumer = location -> {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            if (location == null) {
                AppLogger.d(TAG, "Current location seed returned null provider=" + provider);
                return;
            }
            AppLogger.i(TAG, "Received current location seed provider=" + provider
                    + " location=" + NavigationLocationFormatter.format(
                    AndroidLocationConverter.toNavigationLocation(location)));
            listener.onLocationChanged(location);
        };
        try {
            locationManager.getCurrentLocation(provider, cancellationSignal, locationCallbackExecutor, consumer);
            AppLogger.d(TAG, "Requested current location seed provider=" + provider);
            return cancellationSignal;
        } catch (SecurityException e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Permission denied while requesting current location seed provider=" + provider, e);
            return null;
        } catch (Exception e) {
            clearCurrentLocationCancellation(provider, cancellationSignal);
            AppLogger.w(TAG, "Failed to request current location seed provider=" + provider, e);
            return null;
        }
    }

    private void clearCurrentLocationCancellation(
            @NonNull String provider,
            @NonNull CancellationSignal cancellationSignal
    ) {
        if (NavigationLocationProviders.GPS_PROVIDER.equals(provider)
                && gpsCurrentLocationCancellation == cancellationSignal) {
            gpsCurrentLocationCancellation = null;
        } else if (NavigationLocationProviders.NETWORK_PROVIDER.equals(provider)
                && networkCurrentLocationCancellation == cancellationSignal) {
            networkCurrentLocationCancellation = null;
        }
    }
}
