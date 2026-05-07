package vibro.navigator.about;

import vibro.navigator.R;
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
    @NonNull
    private final HeadingSensorDiagnostic rotationVectorDiagnostic;
    @NonNull
    private final HeadingSensorDiagnostic geomagneticRotationVectorDiagnostic;
    @NonNull
    private final HeadingSensorDiagnostic orientationDiagnostic;
    @NonNull
    private final AboutGnssStatusTracker gnssStatusTracker;

    private boolean started;

    AboutSensorStatusFormatter(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        rotationVectorDiagnostic = new HeadingSensorDiagnostic(
                sensorManager,
                Sensor.TYPE_ROTATION_VECTOR,
                R.string.label_sensor_rotation_vector,
                HeadingSensorValueFormat.ROTATION_VECTOR
        );
        geomagneticRotationVectorDiagnostic = new HeadingSensorDiagnostic(
                sensorManager,
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                R.string.label_sensor_geomagnetic_rotation_vector,
                HeadingSensorValueFormat.ROTATION_VECTOR
        );
        orientationDiagnostic = new HeadingSensorDiagnostic(
                sensorManager,
                Sensor.TYPE_ORIENTATION,
                R.string.label_sensor_orientation,
                HeadingSensorValueFormat.ORIENTATION
        );
        gnssStatusTracker = new AboutGnssStatusTracker(locationManager);
    }

    void start() {
        if (started) {
            return;
        }
        boolean rotationVectorStarted = registerHeadingSensor(rotationVectorDiagnostic);
        boolean geomagneticRotationVectorStarted = registerHeadingSensor(geomagneticRotationVectorDiagnostic);
        boolean orientationStarted = registerHeadingSensor(orientationDiagnostic);
        boolean sensorStarted = rotationVectorStarted || geomagneticRotationVectorStarted || orientationStarted;
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
                rotationVectorDiagnostic.labelResId,
                describeHeadingSensorStatus(rotationVectorDiagnostic),
                describeHeadingSensorValue(rotationVectorDiagnostic)
        );
        appendLine(
                context,
                sb,
                geomagneticRotationVectorDiagnostic.labelResId,
                describeHeadingSensorStatus(geomagneticRotationVectorDiagnostic),
                describeHeadingSensorValue(geomagneticRotationVectorDiagnostic)
        );
        appendLine(
                context,
                sb,
                orientationDiagnostic.labelResId,
                describeHeadingSensorStatus(orientationDiagnostic),
                describeHeadingSensorValue(orientationDiagnostic)
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

    private boolean registerHeadingSensor(@NonNull HeadingSensorDiagnostic diagnostic) {
        return sensorManager != null
                && diagnostic.sensor != null
                && sensorManager.registerListener(this, diagnostic.sensor, SensorManager.SENSOR_DELAY_UI);
    }

    private int describeHeadingSensorStatus(@NonNull HeadingSensorDiagnostic diagnostic) {
        if (sensorManager == null) {
            return R.string.sensor_status_unavailable;
        }
        return diagnostic.sensor == null
                ? R.string.sensor_status_unavailable
                : R.string.sensor_status_available;
    }

    @NonNull
    private String describeHeadingSensorValue(@NonNull HeadingSensorDiagnostic diagnostic) {
        if (diagnostic.sensor == null) {
            return "value=unavailable";
        }
        if (diagnostic.valueFormat == HeadingSensorValueFormat.ORIENTATION) {
            return AboutSensorValueFormatter.describeOrientationValue(
                    diagnostic.latestVector,
                    diagnostic.latestAccuracy,
                    diagnostic.latestElapsedRealtimeMs
            );
        }
        return AboutSensorValueFormatter.describeRotationVectorValue(
                diagnostic.latestVector,
                diagnostic.latestAccuracy,
                diagnostic.latestElapsedRealtimeMs
        );
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        HeadingSensorDiagnostic diagnostic = diagnosticFor(event.sensor.getType());
        if (diagnostic != null) {
            diagnostic.latestVector = event.values.clone();
            diagnostic.latestElapsedRealtimeMs = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        HeadingSensorDiagnostic diagnostic = diagnosticFor(sensor.getType());
        if (diagnostic != null) {
            diagnostic.latestAccuracy = accuracy;
        }
    }

    @Nullable
    private HeadingSensorDiagnostic diagnosticFor(int sensorType) {
        if (rotationVectorDiagnostic.matches(sensorType)) {
            return rotationVectorDiagnostic;
        }
        if (geomagneticRotationVectorDiagnostic.matches(sensorType)) {
            return geomagneticRotationVectorDiagnostic;
        }
        if (orientationDiagnostic.matches(sensorType)) {
            return orientationDiagnostic;
        }
        return null;
    }

    private enum HeadingSensorValueFormat {
        ROTATION_VECTOR,
        ORIENTATION
    }

    private static final class HeadingSensorDiagnostic {
        @Nullable
        final Sensor sensor;
        final int sensorType;
        final int labelResId;
        @NonNull
        final HeadingSensorValueFormat valueFormat;
        @Nullable
        float[] latestVector;
        int latestAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
        long latestElapsedRealtimeMs = -1L;

        HeadingSensorDiagnostic(
                @Nullable SensorManager sensorManager,
                int sensorType,
                int labelResId,
                @NonNull HeadingSensorValueFormat valueFormat
        ) {
            this.sensorType = sensorType;
            this.labelResId = labelResId;
            this.valueFormat = valueFormat;
            sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(sensorType);
        }

        boolean matches(int eventSensorType) {
            return sensor != null && sensorType == eventSensorType;
        }
    }
}
