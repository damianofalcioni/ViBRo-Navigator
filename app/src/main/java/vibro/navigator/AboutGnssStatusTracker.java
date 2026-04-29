package vibro.navigator;

import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

final class AboutGnssStatusTracker {

    @Nullable
    private final LocationManager locationManager;
    @Nullable
    private GnssStatus.Callback callback;
    @Nullable
    private Integer fixedSatelliteCount;

    AboutGnssStatusTracker(@Nullable LocationManager locationManager) {
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
            // Best effort only for developer diagnostics.
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
