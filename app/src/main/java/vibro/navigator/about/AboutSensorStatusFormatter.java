package vibro.navigator.about;

import vibro.navigator.R;
import vibro.navigator.sensor.HeadingSensorSupport;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class AboutSensorStatusFormatter implements SensorEventListener {

    @Nullable
    private final LocationManager locationManager;
    @Nullable
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor headingSensor;
    @NonNull
    private final AboutGnssStatusTracker gnssStatusTracker;

    @Nullable
    private float[] latestGeomagneticVector;
    private int latestGeomagneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private long latestGeomagneticElapsedRealtimeMs = -1L;
    private boolean started;

    AboutSensorStatusFormatter(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        headingSensor = HeadingSensorSupport.findBestSensor(sensorManager);
        gnssStatusTracker = new AboutGnssStatusTracker(locationManager);
    }

    void start() {
        if (started) {
            return;
        }
        boolean sensorStarted = sensorManager != null
                && headingSensor != null
                && sensorManager.registerListener(this, headingSensor, SensorManager.SENSOR_DELAY_UI);
        boolean gnssStarted = gnssStatusTracker.start();
        started = sensorStarted || gnssStarted;
    }

    void stop() {
        if (sensorManager != null && started) {
            sensorManager.unregisterListener(this);
        }
        gnssStatusTracker.stop();
        started = false;
    }

    @NonNull
    String build(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        appendLine(
                context,
                sb,
                R.string.label_sensor_gps_provider,
                describeProviderStatus(LocationManager.GPS_PROVIDER),
                describeLocationValue(LocationManager.GPS_PROVIDER)
        );
        appendLine(
                context,
                sb,
                R.string.label_sensor_network_provider,
                describeProviderStatus(LocationManager.NETWORK_PROVIDER),
                describeLocationValue(LocationManager.NETWORK_PROVIDER)
        );
        appendLine(
                context,
                sb,
                HeadingSensorSupport.labelResIdForSensor(headingSensor),
                describeGeomagneticRotationVectorStatus(),
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

    private int describeProviderStatus(@NonNull String provider) {
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
        return AboutSensorValueFormatter.describeLocationValue(location, gnssStatusTracker.fixedSatelliteCount());
    }

    private int describeGeomagneticRotationVectorStatus() {
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
        return AboutSensorValueFormatter.describeGeomagneticValue(
                latestGeomagneticVector,
                latestGeomagneticAccuracy,
                latestGeomagneticElapsedRealtimeMs
        );
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
