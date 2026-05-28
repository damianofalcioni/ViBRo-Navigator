package vibro.navigator.android.location;

import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationGnssTracker;
import vibro.navigator.nav.location.NavigationLocationProviders;

import java.util.List;

public final class NavigationGnssStatusTracker implements NavigationGnssTracker {

    private static final String TAG = "NavGnssStatus";

    @Nullable
    private final LocationManager locationManager;
    @Nullable
    private GnssStatus.Callback gnssStatusCallback;
    @Nullable
    private Integer fixedSatelliteCount;

    public NavigationGnssStatusTracker(@Nullable LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    @Nullable
    @Override
    public Integer getFixedSatelliteCount() {
        return fixedSatelliteCount;
    }

    @Override
    public void updateForRequestedProviders(@NonNull List<String> requestedProviders) {
        if (shouldTrackGnssStatus(requestedProviders)) {
            ensureTracking();
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        fixedSatelliteCount = null;
        stopTracking();
    }

    private static boolean shouldTrackGnssStatus(@NonNull List<String> requestedProviders) {
        return requestedProviders.contains(NavigationLocationProviders.GPS_PROVIDER)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void ensureTracking() {
        if (locationManager == null || gnssStatusCallback != null) {
            return;
        }
        GnssStatus.Callback callback = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                fixedSatelliteCount = 0;
            }

            @Override
            public void onStopped() {
                fixedSatelliteCount = 0;
            }

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                fixedSatelliteCount = countSatellitesUsedInFix(status);
            }
        };
        try {
            locationManager.registerGnssStatusCallback(callback, new Handler(Looper.getMainLooper()));
            gnssStatusCallback = callback;
            AppLogger.d(TAG, "Registered GNSS status callback");
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Permission denied while registering GNSS status callback", e);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to register GNSS status callback", e);
        }
    }

    private void stopTracking() {
        if (locationManager == null
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                || gnssStatusCallback == null) {
            return;
        }
        try {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            AppLogger.d(TAG, "Unregistered GNSS status callback");
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to unregister GNSS status callback", e);
        } finally {
            gnssStatusCallback = null;
        }
    }

    public static int countSatellitesUsedInFix(boolean... usedInFixFlags) {
        int fixedCount = 0;
        for (boolean usedInFix : usedInFixFlags) {
            if (usedInFix) {
                fixedCount++;
            }
        }
        return fixedCount;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private static int countSatellitesUsedInFix(@NonNull GnssStatus status) {
        boolean[] usedInFixFlags = new boolean[status.getSatelliteCount()];
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            usedInFixFlags[i] = status.usedInFix(i);
        }
        return countSatellitesUsedInFix(usedInFixFlags);
    }
}
