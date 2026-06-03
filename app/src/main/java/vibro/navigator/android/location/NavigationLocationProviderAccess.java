package vibro.navigator.android.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationListener;
import vibro.navigator.nav.location.NavigationLocationProvider;
import vibro.navigator.nav.location.NavigationLocationProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class NavigationLocationProviderAccess implements NavigationLocationProvider {

    private static final String TAG = "NavLocation";

    private final Context context;
    @Nullable
    private final LocationManager locationManager;
    private final LocationListener listener;
    private final Executor locationCallbackExecutor;
    private final NavigationCurrentLocationSeeder currentLocationSeeder;

    public NavigationLocationProviderAccess(
            @NonNull Context context,
            @Nullable LocationManager locationManager,
            @NonNull NavigationLocationListener listener
    ) {
        this.context = context;
        this.locationManager = locationManager;
        this.listener = new AndroidLocationListenerAdapter(listener);
        this.locationCallbackExecutor = ContextCompat.getMainExecutor(context);
        this.currentLocationSeeder = new NavigationCurrentLocationSeeder(
                locationManager,
                this.listener,
                locationCallbackExecutor
        );
    }

    @Override
    public boolean hasFineLocationPermission() {
        return AndroidLocationPermissions.hasFineLocationPermission(context);
    }

    @Override
    public boolean hasCoarseLocationPermission() {
        return AndroidLocationPermissions.hasCoarseLocationPermission(context);
    }

    @Override
    @NonNull
    public List<String> enabledPermittedProviders(boolean fineGranted, boolean coarseGranted) {
        List<String> providers = new ArrayList<>(2);
        addEnabledProviderIfPermitted(
                providers,
                NavigationLocationProviders.GPS_PROVIDER,
                fineGranted,
                coarseGranted
        );
        addEnabledProviderIfPermitted(
                providers,
                NavigationLocationProviders.NETWORK_PROVIDER,
                fineGranted,
                coarseGranted
        );
        return providers;
    }

    @Override
    @NonNull
    public List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs) {
        List<String> requestedProviders = new ArrayList<>(providers.size());
        for (String provider : providers) {
            if (requestProviderUpdates(provider, minTimeMs)) {
                requestedProviders.add(provider);
            }
        }
        return requestedProviders;
    }

    @Override
    @Nullable
    public NavigationLocation getLastKnownLocationQuietly(@NonNull String provider) {
        if (locationManager == null) {
            return null;
        }
        try {
            return AndroidLocationConverter.toNavigationLocation(locationManager.getLastKnownLocation(provider));
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location provider=" + provider, e);
            return null;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            return null;
        }
    }

    @Override
    public void requestCurrentLocationSeeds(boolean fineGranted, boolean coarseGranted) {
        currentLocationSeeder.requestSeeds(fineGranted, coarseGranted);
    }

    @Override
    public void requestSeedForEnabledProvider(@NonNull String provider) {
        currentLocationSeeder.requestSeedForEnabledProvider(provider);
    }

    @Override
    public void cancelPendingCurrentLocationRequests() {
        currentLocationSeeder.cancelPendingCurrentLocationRequests();
    }

    @Override
    public void removeUpdates() {
        removeLegacyUpdates(locationManager, listener);
    }

    @Override
    @NonNull
    public String describeAvailability() {
        if (locationManager == null) {
            return "locationManager=null";
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        boolean gpsEnabled = isProviderEnabled(NavigationLocationProviders.GPS_PROVIDER);
        boolean networkEnabled = isProviderEnabled(NavigationLocationProviders.NETWORK_PROVIDER);
        NavigationLocation lastGps = fineGranted
                ? getLastKnownLocationQuietly(NavigationLocationProviders.GPS_PROVIDER)
                : null;
        NavigationLocation lastNetwork = NavigationLocationProviders.hasAnyLocationPermission(fineGranted, coarseGranted)
                ? getLastKnownLocationQuietly(NavigationLocationProviders.NETWORK_PROVIDER)
                : null;
        return "fineGranted=" + fineGranted
                + ", coarseGranted=" + coarseGranted
                + ", gpsEnabled=" + gpsEnabled
                + ", networkEnabled=" + networkEnabled
                + ", lastGps=" + NavigationLocationFormatter.format(lastGps)
                + ", lastNetwork=" + NavigationLocationFormatter.format(lastNetwork);
    }

    private void addEnabledProviderIfPermitted(
            @NonNull List<String> providers,
            @NonNull String provider,
            boolean fineGranted,
            boolean coarseGranted
    ) {
        if (NavigationLocationProviders.canUseProvider(provider, fineGranted, coarseGranted)
                && isProviderEnabled(provider)) {
            providers.add(provider);
        }
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

    @SuppressLint("MissingPermission")
    private boolean requestProviderUpdates(@NonNull String provider, long minTimeMs) {
        if (locationManager == null) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.requestLocationUpdates(provider, minTimeMs, 0f, locationCallbackExecutor, listener);
            } else {
                locationManager.requestLocationUpdates(provider, minTimeMs, 0f, listener);
            }
            return true;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while requesting location updates provider=" + provider, e);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private static void removeLegacyUpdates(
            @Nullable LocationManager locationManager,
            @NonNull LocationListener listener
    ) {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(listener);
            }
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while removing legacy location updates", e);
        }
    }

    private static final class AndroidLocationListenerAdapter implements LocationListener {
        @NonNull
        private final NavigationLocationListener listener;

        AndroidLocationListenerAdapter(@NonNull NavigationLocationListener listener) {
            this.listener = listener;
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            NavigationLocation navigationLocation = AndroidLocationConverter.toNavigationLocation(location);
            if (navigationLocation != null) {
                listener.onLocationChanged(navigationLocation);
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            listener.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            listener.onProviderDisabled(provider);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onStatusChanged(@Nullable String provider, int status, @Nullable Bundle extras) {
            listener.onProviderStatusChanged(provider, status);
        }
    }
}
