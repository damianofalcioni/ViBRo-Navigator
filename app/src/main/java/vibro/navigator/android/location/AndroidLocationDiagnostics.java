package vibro.navigator.android.location;

import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.location.NavigationLocation;

public final class AndroidLocationDiagnostics {
    @Nullable
    private final LocationManager locationManager;
    @NonNull
    private final AndroidGnssStatusTracker gnssStatusTracker;

    public AndroidLocationDiagnostics(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        gnssStatusTracker = new AndroidGnssStatusTracker(locationManager);
    }

    public static boolean isAnyProviderEnabled(@NonNull Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return false;
        }
        try {
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean startFixedSatelliteTracking() {
        return gnssStatusTracker.start();
    }

    public void stopFixedSatelliteTracking() {
        gnssStatusTracker.stop();
    }

    @Nullable
    public Integer fixedSatelliteCount() {
        return gnssStatusTracker.getFixedSatelliteCount();
    }

    public int providerStatusResId(@NonNull String provider) {
        if (locationManager == null) {
            return R.string.sensor_status_unavailable;
        }
        try {
            return locationManager.isProviderEnabled(provider)
                    ? R.string.sensor_status_enabled
                    : R.string.sensor_status_disabled;
        } catch (SecurityException ignored) {
            return R.string.sensor_status_permission_denied;
        } catch (Exception ignored) {
            return R.string.sensor_status_unavailable;
        }
    }

    @Nullable
    public NavigationLocation lastKnownLocation(@NonNull String provider) throws LocationDiagnosticException {
        if (locationManager == null) {
            return null;
        }
        try {
            return AndroidLocationConverter.toNavigationLocation(locationManager.getLastKnownLocation(provider));
        } catch (SecurityException e) {
            throw new LocationDiagnosticException(LocationDiagnosticError.PERMISSION_DENIED, e);
        } catch (Exception e) {
            throw new LocationDiagnosticException(LocationDiagnosticError.UNAVAILABLE, e);
        }
    }

    public enum LocationDiagnosticError {
        PERMISSION_DENIED,
        UNAVAILABLE
    }

    public static final class LocationDiagnosticException extends Exception {
        @NonNull
        public final LocationDiagnosticError error;

        LocationDiagnosticException(@NonNull LocationDiagnosticError error, @NonNull Throwable cause) {
            super(cause);
            this.error = error;
        }
    }
}
