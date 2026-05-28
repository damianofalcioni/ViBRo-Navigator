package vibro.navigator.android.location;

import android.content.Context;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import vibro.navigator.R;
import vibro.navigator.nav.location.NavigationLocation;

public final class AndroidLocationDiagnostics {
    @Nullable
    private final LocationManager locationManager;
    @NonNull
    private final FixedSatelliteTracker fixedSatelliteTracker;

    public AndroidLocationDiagnostics(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        fixedSatelliteTracker = new FixedSatelliteTracker(locationManager);
    }

    public boolean startFixedSatelliteTracking() {
        return fixedSatelliteTracker.start();
    }

    public void stopFixedSatelliteTracking() {
        fixedSatelliteTracker.stop();
    }

    @Nullable
    public Integer fixedSatelliteCount() {
        return fixedSatelliteTracker.fixedSatelliteCount();
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

    private static final class FixedSatelliteTracker {
        @Nullable
        private final LocationManager locationManager;
        @Nullable
        private GnssStatus.Callback callback;
        @Nullable
        private Integer fixedSatelliteCount;

        FixedSatelliteTracker(@Nullable LocationManager locationManager) {
            this.locationManager = locationManager;
        }

        boolean start() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || locationManager == null || callback != null) {
                return callback != null;
            }
            registerCallback();
            return callback != null;
        }

        void stop() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || locationManager == null || callback == null) {
                return;
            }
            try {
                locationManager.unregisterGnssStatusCallback(callback);
            } catch (Exception ignored) {
                // Best effort only for diagnostics.
            } finally {
                callback = null;
                fixedSatelliteCount = null;
            }
        }

        @Nullable
        Integer fixedSatelliteCount() {
            return fixedSatelliteCount;
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private void registerCallback() {
            GnssStatus.Callback newCallback = new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    fixedSatelliteCount = 0;
                }

                @Override
                public void onStopped() {
                    fixedSatelliteCount = null;
                }

                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    fixedSatelliteCount = countSatellitesUsedInFix(status);
                }
            };
            try {
                locationManager.registerGnssStatusCallback(newCallback, new Handler(Looper.getMainLooper()));
                callback = newCallback;
            } catch (SecurityException ignored) {
                fixedSatelliteCount = null;
            } catch (Exception ignored) {
                fixedSatelliteCount = null;
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private static int countSatellitesUsedInFix(@NonNull GnssStatus status) {
            int fixedCount = 0;
            for (int i = 0; i < status.getSatelliteCount(); i++) {
                if (status.usedInFix(i)) {
                    fixedCount++;
                }
            }
            return fixedCount;
        }
    }
}
