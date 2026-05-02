package vibro.navigator.nav.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class NavigationLocationProviderAccess {

    private static final String TAG = "NavLocation";

    private final Context context;
    @Nullable
    private final LocationManager locationManager;
    private final LocationListener listener;
    private final Executor locationCallbackExecutor;

    public NavigationLocationProviderAccess(
            @NonNull Context context,
            @Nullable LocationManager locationManager,
            @NonNull LocationListener listener
    ) {
        this.context = context;
        this.locationManager = locationManager;
        this.listener = listener;
        this.locationCallbackExecutor = ContextCompat.getMainExecutor(context);
    }

    public boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    public List<String> enabledPermittedProviders(boolean fineGranted, boolean coarseGranted) {
        List<String> providers = new ArrayList<>(2);
        addEnabledProviderIfPermitted(providers, LocationManager.GPS_PROVIDER, fineGranted, coarseGranted);
        addEnabledProviderIfPermitted(providers, LocationManager.NETWORK_PROVIDER, fineGranted, coarseGranted);
        return providers;
    }

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

    @Nullable
    public Location getLastKnownLocationQuietly(@NonNull String provider) {
        if (locationManager == null) {
            return null;
        }
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while reading last known location provider=" + provider, e);
            return null;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            return null;
        }
    }

    @NonNull
    public String describeAvailability() {
        if (locationManager == null) {
            return "locationManager=null";
        }
        boolean fineGranted = hasFineLocationPermission();
        boolean coarseGranted = hasCoarseLocationPermission();
        boolean gpsEnabled = isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return "fineGranted=" + fineGranted
                + ", coarseGranted=" + coarseGranted
                + ", gpsEnabled=" + gpsEnabled
                + ", networkEnabled=" + networkEnabled
                + ", lastGps=" + NavigationLocationFormatter.format(fineGranted
                ? getLastKnownLocationQuietly(LocationManager.GPS_PROVIDER)
                : null)
                + ", lastNetwork=" + NavigationLocationFormatter.format(hasAnyLocationPermission(fineGranted, coarseGranted)
                ? getLastKnownLocationQuietly(LocationManager.NETWORK_PROVIDER)
                : null);
    }

    private void addEnabledProviderIfPermitted(
            @NonNull List<String> providers,
            @NonNull String provider,
            boolean fineGranted,
            boolean coarseGranted
    ) {
        if (canUseProvider(provider, fineGranted, coarseGranted)
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

    public static boolean hasAnyLocationPermission(boolean fineGranted, boolean coarseGranted) {
        return fineGranted || coarseGranted;
    }

    public static boolean canUseProvider(@NonNull String provider, boolean fineGranted, boolean coarseGranted) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            return fineGranted;
        }
        if (LocationManager.NETWORK_PROVIDER.equals(provider)
                || LocationManager.PASSIVE_PROVIDER.equals(provider)) {
            return hasAnyLocationPermission(fineGranted, coarseGranted);
        }
        return hasAnyLocationPermission(fineGranted, coarseGranted);
    }

    @Nullable
    public static String joinProviders(@NonNull List<String> providers) {
        if (providers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) {
                sb.append("+");
            }
            sb.append(providers.get(i));
        }
        return sb.toString();
    }

    public static boolean shouldReuseActiveLocationRequest(
            long minTimeMs,
            @Nullable String providerSummary,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        return providerSummary != null
                && minTimeMs == lastRequestedLocationMinTimeMs
                && providerSummary.equals(lastRequestedProvider);
    }
}
