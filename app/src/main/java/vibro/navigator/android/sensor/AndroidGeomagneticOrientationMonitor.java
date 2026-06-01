package vibro.navigator.android.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.orientation.GeomagneticOrientationMonitor;
import vibro.navigator.nav.orientation.LegacyOrientationAccuracy;
import vibro.navigator.nav.orientation.NavigationHeadingMonitor;
import vibro.navigator.nav.orientation.HeadingAccuracyStatus;
import vibro.navigator.sensor.HeadingSensorSupport;

public final class AndroidGeomagneticOrientationMonitor implements NavigationHeadingMonitor, SensorEventListener {
    private static final String TAG = "GeomagneticHeading";

    @Nullable
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor headingSensor;
    @Nullable
    private final Sensor legacyOrientationSensor;
    @Nullable
    private final GeomagneticOrientationMonitor.Callback callback;
    @NonNull
    private final LegacyOrientationAccuracy legacyOrientationAccuracy = new LegacyOrientationAccuracy();
    @Nullable
    private GeomagneticOrientationMonitor.Sample latestSample;
    private int lastAccuracy = HeadingAccuracyStatus.UNRELIABLE;
    private boolean started;
    private boolean legacyOrientationStarted;

    public AndroidGeomagneticOrientationMonitor(
            @NonNull Context context,
            @Nullable GeomagneticOrientationMonitor.Callback callback
    ) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        headingSensor = AndroidHeadingSensorSupport.findBestSensor(sensorManager);
        legacyOrientationSensor = AndroidHeadingSensorSupport.findLegacyOrientationSensor(sensorManager);
        this.callback = callback;
    }

    @Override
    public boolean start() {
        if (started) {
            return headingSensor != null;
        }
        if (sensorManager == null || headingSensor == null) {
            AppLogger.w(TAG, "Heading sensor unavailable");
            return false;
        }
        if (AndroidHeadingSensorSupport.appSensorType(headingSensor)
                != HeadingSensorSupport.SENSOR_TYPE_ROTATION_VECTOR) {
            AppLogger.i(TAG, "Using " + AndroidHeadingSensorSupport.describeSensor(headingSensor) + " fallback");
        }
        started = sensorManager.registerListener(this, headingSensor, SensorManager.SENSOR_DELAY_UI);
        if (!started) {
            AppLogger.w(TAG, "Failed to register heading listener");
            return false;
        }
        registerLegacyOrientationSensor();
        return true;
    }

    @Override
    public void stop() {
        if (sensorManager != null && (started || legacyOrientationStarted)) {
            sensorManager.unregisterListener(this);
        }
        started = false;
        legacyOrientationStarted = false;
        latestSample = null;
        lastAccuracy = HeadingAccuracyStatus.UNRELIABLE;
        legacyOrientationAccuracy.reset();
    }

    @Override
    @Nullable
    public GeomagneticOrientationMonitor.Sample getLatestSample() {
        return latestSample;
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (AndroidHeadingSensorSupport.matchesSelectedSensor(headingSensor, event.sensor.getType())) {
            updateHeadingSample(event);
            return;
        }
        if (AndroidHeadingSensorSupport.matchesLegacyOrientationSensor(legacyOrientationSensor, event.sensor.getType())) {
            refreshLegacyOrientationAccuracyTimestamp(SystemClock.elapsedRealtime());
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        if (AndroidHeadingSensorSupport.matchesSelectedSensor(headingSensor, sensor.getType())) {
            lastAccuracy = AndroidHeadingSensorSupport.toHeadingAccuracyStatus(accuracy);
            return;
        }
        if (AndroidHeadingSensorSupport.matchesLegacyOrientationSensor(legacyOrientationSensor, sensor.getType())) {
            rememberLegacyOrientationAccuracy(
                    AndroidHeadingSensorSupport.toHeadingAccuracyStatus(accuracy),
                    SystemClock.elapsedRealtime()
            );
        }
    }

    private void updateHeadingSample(@NonNull SensorEvent event) {
        long nowElapsedRealtimeMs = SystemClock.elapsedRealtime();
        AndroidRotationVectorOrientation orientation =
                AndroidRotationVectorOrientation.fromRotationVector(event.values);
        latestSample = new GeomagneticOrientationMonitor.Sample(
                orientation.headingDegrees,
                orientation.pitchDegrees,
                orientation.rollDegrees,
                lastAccuracy,
                legacyOrientationAccuracy.freshAccuracy(nowElapsedRealtimeMs),
                legacyOrientationAccuracy.freshElapsedRealtimeMs(nowElapsedRealtimeMs),
                orientation.headingAccuracyDegrees,
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
