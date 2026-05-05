package vibro.navigator.map;

import vibro.navigator.R;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class MapPickerLocationController {

    private static final String TAG = "MapPickerLocation";
    private static final int REQUEST_LOCATION_PERMISSION = 4001;
    private static final long LOCATION_TIMEOUT_MS = 10_000L;

    interface Callback {
        void onCurrentLocation(@NonNull Location location, boolean selectPoint);

        void onLocationMessage(@StringRes int messageResId);
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final Callback callback;
    @Nullable
    private final LocationManager locationManager;
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Runnable locationTimeoutRunnable;
    @NonNull
    private final LocationListener singleFixListener;

    private boolean requestingLocationUpdate;
    private boolean pendingCurrentLocationSelection = true;

    MapPickerLocationController(@NonNull Activity activity, @NonNull Callback callback) {
        this.activity = activity;
        this.callback = callback;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationTimeoutRunnable = () -> {
            stopLocationUpdates();
            callback.onLocationMessage(R.string.msg_map_location_unavailable);
            AppLogger.w(TAG, "Timed out waiting for a current location fix");
        };
        singleFixListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                AppLogger.i(TAG, "Received current location provider=" + safeProvider(location)
                        + " lat=" + location.getLatitude()
                        + " lon=" + location.getLongitude());
                boolean selectPoint = pendingCurrentLocationSelection;
                stopLocationUpdates();
                callback.onCurrentLocation(location, selectPoint);
            }
        };
    }

    void centerOnCurrentLocation(boolean selectPoint) {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        Location bestLastKnownLocation = findBestLastKnownLocation();
        if (bestLastKnownLocation != null) {
            AppLogger.i(TAG, "Using last known location provider=" + safeProvider(bestLastKnownLocation));
            callback.onCurrentLocation(bestLastKnownLocation, selectPoint);
            return;
        }

        if (locationManager == null) {
            callback.onLocationMessage(R.string.msg_map_location_unavailable);
            return;
        }

        List<String> providers = new ArrayList<>();
        addEnabledProvider(providers, LocationManager.GPS_PROVIDER);
        addEnabledProvider(providers, LocationManager.NETWORK_PROVIDER);
        if (providers.isEmpty()) {
            callback.onLocationMessage(R.string.msg_location_disabled);
            return;
        }

        requestFreshLocation(providers, selectPoint);
    }

    boolean onRequestPermissionsResult(int requestCode) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return false;
        }
        if (hasLocationPermission()) {
            centerOnCurrentLocation(pendingCurrentLocationSelection);
            return true;
        }
        callback.onLocationMessage(R.string.msg_permission_required);
        return true;
    }

    void stopLocationUpdates() {
        mainHandler.removeCallbacks(locationTimeoutRunnable);
        if (!requestingLocationUpdate || locationManager == null) {
            requestingLocationUpdate = false;
            pendingCurrentLocationSelection = true;
            return;
        }
        try {
            locationManager.removeUpdates(singleFixListener);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to remove location updates", e);
        }
        requestingLocationUpdate = false;
        pendingCurrentLocationSelection = true;
    }

    private void requestFreshLocation(@NonNull List<String> providers, boolean selectPoint) {
        stopLocationUpdates();
        try {
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, 0L, 0f, singleFixListener, Looper.getMainLooper());
            }
            pendingCurrentLocationSelection = selectPoint;
            requestingLocationUpdate = true;
            mainHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT_MS);
            callback.onLocationMessage(R.string.msg_map_location_searching);
            AppLogger.i(TAG, "Requested fresh location from providers=" + providers);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to request current location updates", e);
            callback.onLocationMessage(R.string.msg_permission_required);
        }
    }

    private void addEnabledProvider(@NonNull List<String> providers, @NonNull String provider) {
        if (isProviderEnabled(provider)) {
            providers.add(provider);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location findBestLastKnownLocation() {
        if (locationManager == null || !hasLocationPermission()) {
            return null;
        }
        Location best = null;
        String[] providers = new String[]{
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };
        for (String provider : providers) {
            best = betterLastKnownLocation(provider, best);
        }
        return best;
    }

    @Nullable
    private Location betterLastKnownLocation(@NonNull String provider, @Nullable Location best) {
        try {
            Location candidate = locationManager.getLastKnownLocation(provider);
            if (candidate == null) {
                return best;
            }
            return best == null || isBetterLocation(candidate, best) ? candidate : best;
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to read last known location provider=" + provider, e);
            return best;
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

    private boolean isBetterLocation(@NonNull Location candidate, @NonNull Location best) {
        if (candidate.hasAccuracy() && best.hasAccuracy()) {
            float accuracyDelta = candidate.getAccuracy() - best.getAccuracy();
            if (accuracyDelta < -10f) {
                return true;
            }
            if (accuracyDelta > 10f) {
                return false;
            }
        }
        return candidate.getTime() > best.getTime();
    }

    @NonNull
    private static String safeProvider(@NonNull Location location) {
        String provider = location.getProvider();
        return provider == null ? "unknown" : provider;
    }
}

