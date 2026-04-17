package vibro.navigator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Locale;

final class AboutSensorStatusFormatter implements SensorEventListener {

    @Nullable
    private final LocationManager locationManager;
    @Nullable
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor headingSensor;
    @Nullable
    private GnssStatus.Callback gnssStatusCallback;

    @Nullable
    private float[] latestGeomagneticVector;
    private int latestGeomagneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private long latestGeomagneticElapsedRealtimeMs = -1L;
    @Nullable
    private Integer latestFixedSatelliteCount;
    private boolean started;

    AboutSensorStatusFormatter(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        headingSensor = HeadingSensorSupport.findBestSensor(sensorManager);
    }

    void start() {
        if (started) {
            return;
        }
        boolean sensorStarted = sensorManager != null
                && headingSensor != null
                && sensorManager.registerListener(this, headingSensor, SensorManager.SENSOR_DELAY_UI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerGnssStatusCallback();
        }
        started = sensorStarted || gnssStatusCallback != null;
    }

    void stop() {
        if (sensorManager != null && started) {
            sensorManager.unregisterListener(this);
        }
        unregisterGnssStatusCallback();
        started = false;
    }

    @NonNull
    String build(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        appendLine(
                context,
                sb,
                R.string.label_sensor_gps_provider,
                describeProviderStatus(context, LocationManager.GPS_PROVIDER),
                describeLocationValue(LocationManager.GPS_PROVIDER)
        );
        appendLine(
                context,
                sb,
                R.string.label_sensor_network_provider,
                describeProviderStatus(context, LocationManager.NETWORK_PROVIDER),
                describeLocationValue(LocationManager.NETWORK_PROVIDER)
        );
        appendLine(
                context,
                sb,
                HeadingSensorSupport.labelResIdForSensor(headingSensor),
                describeGeomagneticRotationVectorStatus(context),
                describeGeomagneticValue()
        );
        return sb.toString();
    }

    private void appendLine(
            @NonNull Context context,
            @NonNull StringBuilder sb,
            int labelResId,
            int statusResId,
            @NonNull String value
    ) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(context.getString(
                R.string.format_about_sensor_status_detail,
                context.getString(labelResId),
                context.getString(statusResId),
                value
        ));
    }

    private int describeProviderStatus(@NonNull Context context, @NonNull String provider) {
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

    @NonNull
    private String describeLocationValue(@NonNull String provider) {
        if (locationManager == null) {
            return "value=none";
        }
        final Location location;
        try {
            location = locationManager.getLastKnownLocation(provider);
        } catch (SecurityException ignored) {
            return "value=permission denied";
        } catch (Exception ignored) {
            return "value=unavailable";
        }
        if (location == null) {
            return "value=none";
        }
        return describeLocationValue(location, latestFixedSatelliteCount);
    }

    @NonNull
    static String describeLocationValue(@NonNull Location location, @Nullable Integer fixedSatelliteCount) {
        StringBuilder sb = new StringBuilder("value=");
        sb.append(String.format(
                Locale.US,
                "lat=%.6f lon=%.6f",
                location.getLatitude(),
                location.getLongitude()
        ));
        if (location.hasAccuracy()) {
            sb.append(String.format(Locale.US, " acc=%.1fm", location.getAccuracy()));
        }
        if (location.hasAltitude()) {
            sb.append(String.format(Locale.US, " alt=%.1fm", location.getAltitude()));
        }
        if (location.hasSpeed()) {
            sb.append(String.format(Locale.US, " speed=%.1fkm/h", location.getSpeed() * 3.6f));
        }
        if (location.hasBearing()) {
            sb.append(String.format(Locale.US, " bearing=%.0fdeg", location.getBearing()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
            sb.append(String.format(Locale.US, " bearingAcc=%.0fdeg", location.getBearingAccuracyDegrees()));
        }
        if (fixedSatelliteCount != null && fixedSatelliteCount >= 0) {
            sb.append(" sats=").append(fixedSatelliteCount);
        }
        long ageSeconds = Math.max(0L, (System.currentTimeMillis() - location.getTime()) / 1000L);
        sb.append(" age=").append(ageSeconds).append("s");
        return sb.toString();
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void registerGnssStatusCallback() {
        if (locationManager == null || gnssStatusCallback != null) {
            return;
        }
        GnssStatus.Callback callback = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                latestFixedSatelliteCount = 0;
            }

            @Override
            public void onStopped() {
                latestFixedSatelliteCount = null;
            }

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                latestFixedSatelliteCount = countSatellitesUsedInFix(status);
            }
        };
        try {
            locationManager.registerGnssStatusCallback(callback, new Handler(Looper.getMainLooper()));
            gnssStatusCallback = callback;
        } catch (SecurityException ignored) {
            latestFixedSatelliteCount = null;
        } catch (Exception ignored) {
            latestFixedSatelliteCount = null;
        }
    }

    private void unregisterGnssStatusCallback() {
        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N || gnssStatusCallback == null) {
            return;
        }
        try {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        } catch (Exception ignored) {
            // Best effort only for developer diagnostics.
        } finally {
            gnssStatusCallback = null;
            latestFixedSatelliteCount = null;
        }
    }

    private int describeGeomagneticRotationVectorStatus(@NonNull Context context) {
        if (sensorManager == null) {
            return R.string.sensor_status_unavailable;
        }
        return headingSensor == null
                ? R.string.sensor_status_unavailable
                : R.string.sensor_status_available;
    }

    @NonNull
    private String describeGeomagneticValue() {
        if (headingSensor == null) {
            return "value=unavailable";
        }
        if (latestGeomagneticVector == null || latestGeomagneticElapsedRealtimeMs < 0L) {
            return "value=waiting for sample";
        }

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, latestGeomagneticVector);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        double headingDegrees = (Math.toDegrees(orientation[0]) + 360.0) % 360.0;
        double pitchDegrees = Math.toDegrees(orientation[1]);
        double rollDegrees = Math.toDegrees(orientation[2]);
        String headingAccuracyValue = describeHeadingAccuracy(latestGeomagneticVector);
        long ageMs = Math.max(0L, SystemClock.elapsedRealtime() - latestGeomagneticElapsedRealtimeMs);

        return String.format(
                Locale.US,
                "value=heading=%.0fdeg pitch=%.0fdeg roll=%.0fdeg headingAcc=%s acc=%s age=%dms raw=%s",
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                headingAccuracyValue,
                accuracyLabel(latestGeomagneticAccuracy),
                ageMs,
                formatVector(latestGeomagneticVector)
        );
    }

    @NonNull
    private static String describeHeadingAccuracy(@NonNull float[] values) {
        if (values.length <= 4) {
            return "missing";
        }
        float headingAccuracyRadians = values[4];
        if (!Float.isFinite(headingAccuracyRadians)) {
            return "invalid";
        }
        if (headingAccuracyRadians < 0f) {
            return "unreliable";
        }
        return String.format(Locale.US, "%.1fdeg", Math.toDegrees(headingAccuracyRadians));
    }

    @NonNull
    private static String accuracyLabel(int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "low";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "medium";
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "high";
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
            default:
                return "unreliable";
        }
    }

    @NonNull
    private static String formatVector(@NonNull float[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.US, "%.3f", values[i]));
        }
        sb.append("]");
        return sb.toString();
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

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (!HeadingSensorSupport.matchesSelectedSensor(headingSensor, event.sensor.getType())) {
            return;
        }
        latestGeomagneticVector = event.values.clone();
        latestGeomagneticElapsedRealtimeMs = SystemClock.elapsedRealtime();
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        if (HeadingSensorSupport.matchesSelectedSensor(headingSensor, sensor.getType())) {
            latestGeomagneticAccuracy = accuracy;
        }
    }
}
