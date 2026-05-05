package vibro.navigator.nav.orientation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.sensor.HeadingSensorSupport;
import vibro.navigator.util.AppLogger;

public final class GeomagneticOrientationMonitor implements SensorEventListener {

    public interface Callback {
        void onSampleUpdated(@NonNull Sample sample);
    }

    public static final class Sample {
        private static final double MAX_FLAT_TILT_DEGREES = 25.0;

        final double headingDegrees;
        final double pitchDegrees;
        final double rollDegrees;
        final int accuracy;
        @Nullable
        final Double headingAccuracyDegrees;
        final long elapsedRealtimeMs;

        public Sample(
                double headingDegrees,
                double pitchDegrees,
                double rollDegrees,
                int accuracy,
                @Nullable Double headingAccuracyDegrees,
                long elapsedRealtimeMs
        ) {
            this.headingDegrees = headingDegrees;
            this.pitchDegrees = pitchDegrees;
            this.rollDegrees = rollDegrees;
            this.accuracy = accuracy;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            this.elapsedRealtimeMs = elapsedRealtimeMs;
        }

        public boolean isFlatEnough() {
            return Math.abs(pitchDegrees) <= MAX_FLAT_TILT_DEGREES
                    && Math.abs(rollDegrees) <= MAX_FLAT_TILT_DEGREES;
        }

        public boolean isAccuracyHighEnough() {
            return accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
        }

        public boolean isHeadingAccuracyHighEnough(double absoluteTurnDegrees, double minimumTurnDegrees) {
            return headingAccuracyDegrees == null
                    || absoluteTurnDegrees - headingAccuracyDegrees >= minimumTurnDegrees;
        }
    }

    private static final String TAG = "GeomagneticHeading";

    @Nullable
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor orientationSensor;
    @Nullable
    private final Callback callback;
    @Nullable
    private Sample latestSample;
    private int lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private boolean started;

    public GeomagneticOrientationMonitor(@NonNull Context context, @Nullable Callback callback) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = HeadingSensorSupport.findBestSensor(sensorManager);
        this.callback = callback;
    }

    public boolean start() {
        if (started) {
            return orientationSensor != null;
        }
        if (sensorManager == null || orientationSensor == null) {
            AppLogger.w(TAG, "Heading sensor unavailable");
            return false;
        }
        if (orientationSensor.getType() != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            AppLogger.i(TAG, "Using " + HeadingSensorSupport.describeSensor(orientationSensor) + " fallback");
        }
        started = sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
        if (!started) {
            AppLogger.w(TAG, "Failed to register geomagnetic heading listener");
        }
        return started;
    }

    public void stop() {
        if (sensorManager != null && started) {
            sensorManager.unregisterListener(this);
        }
        started = false;
        latestSample = null;
        lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    }

    @Nullable
    public Sample getLatestSample() {
        return latestSample;
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (!HeadingSensorSupport.matchesSelectedSensor(orientationSensor, event.sensor.getType())) {
            return;
        }
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        double headingDegrees = (Math.toDegrees(orientation[0]) + 360.0) % 360.0;
        double pitchDegrees = Math.toDegrees(orientation[1]);
        double rollDegrees = Math.toDegrees(orientation[2]);
        Double headingAccuracyDegrees = null;
        if (event.values.length > 4) {
            float headingAccuracyRadians = event.values[4];
            if (Float.isFinite(headingAccuracyRadians) && headingAccuracyRadians >= 0f) {
                headingAccuracyDegrees = Math.toDegrees(headingAccuracyRadians);
            }
        }
        latestSample = new Sample(
                headingDegrees,
                pitchDegrees,
                rollDegrees,
                lastAccuracy,
                headingAccuracyDegrees,
                SystemClock.elapsedRealtime()
        );
        if (callback != null) {
            callback.onSampleUpdated(latestSample);
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        if (HeadingSensorSupport.matchesSelectedSensor(orientationSensor, sensor.getType())) {
            lastAccuracy = accuracy;
        }
    }
}
