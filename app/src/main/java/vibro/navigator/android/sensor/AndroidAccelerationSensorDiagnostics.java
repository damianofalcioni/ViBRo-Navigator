package vibro.navigator.android.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.orientation.HeadingAccuracyStatus;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

public final class AndroidAccelerationSensorDiagnostics implements SensorEventListener {
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

    @Nullable
    private final SensorManager sensorManager;
    @NonNull
    private final List<Diagnostic> diagnostics;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private boolean started;

    public AndroidAccelerationSensorDiagnostics(@NonNull Context context) {
        this(context, AndroidElapsedRealtimeClock.INSTANCE);
    }

    AndroidAccelerationSensorDiagnostics(
            @NonNull Context context,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        diagnostics = buildDiagnostics(sensorManager);
        this.elapsedRealtimeClock = elapsedRealtimeClock;
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
        long nowElapsedRealtimeMs = elapsedRealtimeClock.elapsedRealtimeMs();
        for (Diagnostic diagnostic : diagnostics) {
            out.add(new Snapshot(
                    diagnostic.labelResId,
                    diagnostic.sensor != null,
                    describeValue(diagnostic, nowElapsedRealtimeMs)
            ));
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        Diagnostic diagnostic = diagnosticFor(event.sensor.getType());
        if (diagnostic != null) {
            diagnostic.latestVector = event.values.clone();
            diagnostic.latestElapsedRealtimeMs = elapsedRealtimeClock.elapsedRealtimeMs();
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
        List<Diagnostic> out = new ArrayList<>(2);
        out.add(new Diagnostic(
                sensorManager,
                Sensor.TYPE_LINEAR_ACCELERATION,
                R.string.label_sensor_linear_acceleration
        ));
        out.add(new Diagnostic(
                sensorManager,
                Sensor.TYPE_ACCELEROMETER,
                R.string.label_sensor_accelerometer
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
    private static String describeValue(@NonNull Diagnostic diagnostic, long nowElapsedRealtimeMs) {
        if (diagnostic.sensor == null) {
            return "value=unavailable";
        }
        return AndroidAccelerationSensorValueFormatter.describeValue(
                diagnostic.latestVector,
                diagnostic.latestAccuracy,
                diagnostic.latestElapsedRealtimeMs,
                nowElapsedRealtimeMs
        );
    }

    private static final class Diagnostic {
        @Nullable
        final Sensor sensor;
        final int sensorType;
        final int labelResId;
        @Nullable
        float[] latestVector;
        int latestAccuracy = HeadingAccuracyStatus.UNRELIABLE;
        long latestElapsedRealtimeMs = -1L;

        Diagnostic(@Nullable SensorManager sensorManager, int sensorType, int labelResId) {
            this.sensorType = sensorType;
            this.labelResId = labelResId;
            sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(sensorType);
        }

        boolean matches(int eventSensorType) {
            return sensor != null && sensor.getType() == eventSensorType;
        }
    }
}
