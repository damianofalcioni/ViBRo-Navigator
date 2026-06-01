package vibro.navigator.android.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.orientation.HeadingAccuracyStatus;
import vibro.navigator.sensor.HeadingSensorSupport;

public final class AndroidHeadingSensorDiagnostics implements SensorEventListener {
    public static final class Snapshot {
        public final int labelResId;
        public final boolean available;
        @NonNull
        public final String value;

        private Snapshot(int labelResId, boolean available, @NonNull String value) {
            this.labelResId = labelResId;
            this.available = available;
            this.value = value;
        }
    }

    private static final int VALUE_FORMAT_ROTATION_VECTOR = 1;
    private static final int VALUE_FORMAT_ORIENTATION = 2;

    @Nullable
    private final SensorManager sensorManager;
    @NonNull
    private final List<Diagnostic> diagnostics;
    private boolean started;

    public AndroidHeadingSensorDiagnostics(@NonNull Context context) {
        sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        diagnostics = buildDiagnostics(sensorManager);
    }

    public boolean start() {
        if (started) {
            return true;
        }
        boolean sensorStarted = false;
        for (Diagnostic diagnostic : diagnostics) {
            sensorStarted |= register(diagnostic);
        }
        started = sensorStarted;
        return sensorStarted;
    }

    public void stop() {
        if (sensorManager != null && started) {
            sensorManager.unregisterListener(this);
        }
        started = false;
    }

    @NonNull
    public List<Snapshot> snapshots() {
        List<Snapshot> out = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            out.add(new Snapshot(
                    diagnostic.labelResId,
                    diagnostic.sensor != null,
                    describeValue(diagnostic)
            ));
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        Diagnostic diagnostic = diagnosticFor(event.sensor.getType());
        if (diagnostic != null) {
            diagnostic.latestVector = event.values.clone();
            diagnostic.latestElapsedRealtimeMs = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
        Diagnostic diagnostic = diagnosticFor(sensor.getType());
        if (diagnostic != null) {
            diagnostic.latestAccuracy = AndroidHeadingSensorSupport.toHeadingAccuracyStatus(accuracy);
        }
    }

    @NonNull
    private static List<Diagnostic> buildDiagnostics(@Nullable SensorManager sensorManager) {
        List<Diagnostic> out = new ArrayList<>(3);
        out.add(new Diagnostic(
                sensorManager,
                HeadingSensorSupport.SENSOR_TYPE_ROTATION_VECTOR,
                VALUE_FORMAT_ROTATION_VECTOR
        ));
        out.add(new Diagnostic(
                sensorManager,
                HeadingSensorSupport.SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                VALUE_FORMAT_ROTATION_VECTOR
        ));
        out.add(new Diagnostic(
                sensorManager,
                HeadingSensorSupport.SENSOR_TYPE_LEGACY_ORIENTATION,
                VALUE_FORMAT_ORIENTATION
        ));
        return out;
    }

    private boolean register(@NonNull Diagnostic diagnostic) {
        return sensorManager != null
                && diagnostic.sensor != null
                && sensorManager.registerListener(this, diagnostic.sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Nullable
    private Diagnostic diagnosticFor(int sensorType) {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.matches(sensorType)) {
                return diagnostic;
            }
        }
        return null;
    }

    @NonNull
    private static String describeValue(@NonNull Diagnostic diagnostic) {
        if (diagnostic.sensor == null) {
            return "value=unavailable";
        }
        if (diagnostic.valueFormat == VALUE_FORMAT_ORIENTATION) {
            return AndroidHeadingSensorValueFormatter.describeOrientationValue(
                    diagnostic.latestVector,
                    diagnostic.latestAccuracy,
                    diagnostic.latestElapsedRealtimeMs
            );
        }
        return AndroidHeadingSensorValueFormatter.describeRotationVectorValue(
                diagnostic.latestVector,
                diagnostic.latestAccuracy,
                diagnostic.latestElapsedRealtimeMs
        );
    }

    private static final class Diagnostic {
        @Nullable
        final Sensor sensor;
        final int sensorType;
        final int labelResId;
        final int valueFormat;
        @Nullable
        float[] latestVector;
        int latestAccuracy = HeadingAccuracyStatus.UNRELIABLE;
        long latestElapsedRealtimeMs = -1L;

        Diagnostic(
                @Nullable SensorManager sensorManager,
                int sensorType,
                int valueFormat
        ) {
            this.sensorType = sensorType;
            labelResId = HeadingSensorSupport.labelResIdForSensorType(sensorType);
            this.valueFormat = valueFormat;
            sensor = AndroidHeadingSensorSupport.defaultSensor(sensorManager, sensorType);
        }

        boolean matches(int eventSensorType) {
            return sensor != null && sensor.getType() == eventSensorType;
        }
    }
}
