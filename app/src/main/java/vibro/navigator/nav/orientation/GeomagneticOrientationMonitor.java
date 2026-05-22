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
import vibro.navigator.logging.AppLogger;

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
        final Integer legacyOrientationAccuracy;
        final long legacyOrientationAccuracyElapsedRealtimeMs;
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
            this(
                    headingDegrees,
                    pitchDegrees,
                    rollDegrees,
                    accuracy,
                    null,
                    -1L,
                    headingAccuracyDegrees,
                    elapsedRealtimeMs
            );
        }

        public Sample(
                double headingDegrees,
                double pitchDegrees,
                double rollDegrees,
                int accuracy,
                @Nullable Integer legacyOrientationAccuracy,
                long legacyOrientationAccuracyElapsedRealtimeMs,
                @Nullable Double headingAccuracyDegrees,
                long elapsedRealtimeMs
        ) {
            this.headingDegrees = headingDegrees;
            this.pitchDegrees = pitchDegrees;
            this.rollDegrees = rollDegrees;
            this.accuracy = accuracy;
            this.legacyOrientationAccuracy = legacyOrientationAccuracy;
            this.legacyOrientationAccuracyElapsedRealtimeMs = legacyOrientationAccuracyElapsedRealtimeMs;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
            this.elapsedRealtimeMs = elapsedRealtimeMs;
        }

        @NonNull
        Sample withLegacyOrientationAccuracy(
                @Nullable Integer updatedLegacyOrientationAccuracy,
                long updatedLegacyOrientationAccuracyElapsedRealtimeMs
        ) {
            return new Sample(
                    headingDegrees,
                    pitchDegrees,
                    rollDegrees,
                    accuracy,
                    updatedLegacyOrientationAccuracy,
                    updatedLegacyOrientationAccuracyElapsedRealtimeMs,
                    headingAccuracyDegrees,
                    elapsedRealtimeMs
            );
        }

        public boolean isFlatEnough() {
            return Math.abs(pitchDegrees) <= MAX_FLAT_TILT_DEGREES
                    && Math.abs(rollDegrees) <= MAX_FLAT_TILT_DEGREES;
        }

        public boolean isAccuracyHighEnough(long nowElapsedRealtimeMs) {
            return HeadingAccuracyPolicy.isAccuracyHighEnough(
                    accuracy,
                    legacyOrientationAccuracy,
                    legacyOrientationAccuracyElapsedRealtimeMs,
                    nowElapsedRealtimeMs
            );
        }

        public boolean isHeadingAccuracyHighEnough(
                double absoluteTurnDegrees,
                double minimumTurnDegrees,
                long nowElapsedRealtimeMs
        ) {
            Double effectiveHeadingAccuracyDegrees = effectiveHeadingAccuracyDegrees(nowElapsedRealtimeMs);
            return effectiveHeadingAccuracyDegrees == null
                    || absoluteTurnDegrees - effectiveHeadingAccuracyDegrees >= minimumTurnDegrees;
        }

        @Nullable
        public Double effectiveHeadingAccuracyDegrees(long nowElapsedRealtimeMs) {
            return HeadingAccuracyPolicy.effectiveHeadingAccuracyDegrees(
                    accuracy,
                    headingAccuracyDegrees,
                    legacyOrientationAccuracy,
                    legacyOrientationAccuracyElapsedRealtimeMs,
                    nowElapsedRealtimeMs
            );
        }
    }

    private static final String TAG = "GeomagneticHeading";

    @Nullable
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor headingSensor;
    @Nullable
    private final Sensor legacyOrientationSensor;
    @Nullable
    private final Callback callback;
    @NonNull
    private final LegacyOrientationAccuracy legacyOrientationAccuracy = new LegacyOrientationAccuracy();
    @Nullable
    private Sample latestSample;
    private int lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private boolean started;
    private boolean legacyOrientationStarted;

    public GeomagneticOrientationMonitor(@NonNull Context context, @Nullable Callback callback) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        headingSensor = HeadingSensorSupport.findBestSensor(sensorManager);
        legacyOrientationSensor = HeadingSensorSupport.findLegacyOrientationSensor(sensorManager);
        this.callback = callback;
    }

    public boolean start() {
        if (started) {
            return headingSensor != null;
        }
        if (sensorManager == null || headingSensor == null) {
            AppLogger.w(TAG, "Heading sensor unavailable");
            return false;
        }
        if (headingSensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            AppLogger.i(TAG, "Using " + HeadingSensorSupport.describeSensor(headingSensor) + " fallback");
        }
        started = sensorManager.registerListener(this, headingSensor, SensorManager.SENSOR_DELAY_UI);
        if (!started) {
            AppLogger.w(TAG, "Failed to register heading listener");
            return false;
        }
        registerLegacyOrientationSensor();
        return started;
    }

    public void stop() {
        if (sensorManager != null && (started || legacyOrientationStarted)) {
            sensorManager.unregisterListener(this);
        }
        started = false;
        legacyOrientationStarted = false;
        latestSample = null;
        lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
        legacyOrientationAccuracy.reset();
    }

    @Nullable
    public Sample getLatestSample() {
        return latestSample;
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (HeadingSensorSupport.matchesSelectedSensor(headingSensor, event.sensor.getType())) {
            updateHeadingSample(event);
            return;
        }
        if (HeadingSensorSupport.matchesLegacyOrientationSensor(legacyOrientationSensor, event.sensor.getType())) {
            refreshLegacyOrientationAccuracyTimestamp(SystemClock.elapsedRealtime());
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        if (HeadingSensorSupport.matchesSelectedSensor(headingSensor, sensor.getType())) {
            lastAccuracy = accuracy;
            return;
        }
        if (HeadingSensorSupport.matchesLegacyOrientationSensor(legacyOrientationSensor, sensor.getType())) {
            rememberLegacyOrientationAccuracy(accuracy, SystemClock.elapsedRealtime());
        }
    }

    private void updateHeadingSample(@NonNull SensorEvent event) {
        long nowElapsedRealtimeMs = SystemClock.elapsedRealtime();
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
                legacyOrientationAccuracy.freshAccuracy(nowElapsedRealtimeMs),
                legacyOrientationAccuracy.freshElapsedRealtimeMs(nowElapsedRealtimeMs),
                headingAccuracyDegrees,
                nowElapsedRealtimeMs
        );
        notifySampleUpdated();
    }

    private void registerLegacyOrientationSensor() {
        if (sensorManager == null || legacyOrientationSensor == null) {
            return;
        }
        legacyOrientationStarted = sensorManager.registerListener(
                this,
                legacyOrientationSensor,
                SensorManager.SENSOR_DELAY_UI
        );
        if (legacyOrientationStarted) {
            AppLogger.i(TAG, "Using deprecated orientation sensor as heading calibration cross-check");
        }
    }

    private void rememberLegacyOrientationAccuracy(int accuracy, long nowElapsedRealtimeMs) {
        legacyOrientationAccuracy.remember(accuracy, nowElapsedRealtimeMs);
        refreshLatestSampleLegacyAccuracy(nowElapsedRealtimeMs);
    }

    private void refreshLegacyOrientationAccuracyTimestamp(long nowElapsedRealtimeMs) {
        if (legacyOrientationAccuracy.refreshTimestamp(nowElapsedRealtimeMs)) {
            refreshLatestSampleLegacyAccuracy(nowElapsedRealtimeMs);
        }
    }

    private void refreshLatestSampleLegacyAccuracy(long nowElapsedRealtimeMs) {
        if (latestSample == null) {
            return;
        }
        latestSample = latestSample.withLegacyOrientationAccuracy(
                legacyOrientationAccuracy.freshAccuracy(nowElapsedRealtimeMs),
                legacyOrientationAccuracy.freshElapsedRealtimeMs(nowElapsedRealtimeMs)
        );
        notifySampleUpdated();
    }

    private void notifySampleUpdated() {
        if (callback != null && latestSample != null) {
            callback.onSampleUpdated(latestSample);
        }
    }
}
