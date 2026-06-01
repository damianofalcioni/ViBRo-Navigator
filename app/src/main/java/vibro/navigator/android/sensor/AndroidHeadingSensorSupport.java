package vibro.navigator.android.sensor;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;
import vibro.navigator.sensor.HeadingSensorSupport;

final class AndroidHeadingSensorSupport {
    // Keep the legacy fallback explicit for devices without rotation-vector sensors.
    @SuppressWarnings("deprecation")
    private static final int LEGACY_ORIENTATION_SENSOR_TYPE = Sensor.TYPE_ORIENTATION;

    private AndroidHeadingSensorSupport() {
    }

    @Nullable
    static Sensor findBestSensor(@Nullable SensorManager sensorManager) {
        if (sensorManager == null) {
            return null;
        }
        int sensorType = HeadingSensorSupport.chooseBestSensorType(
                sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        );
        return defaultSensor(sensorManager, sensorType);
    }

    @Nullable
    static Sensor findLegacyOrientationSensor(@Nullable SensorManager sensorManager) {
        return sensorManager == null ? null : sensorManager.getDefaultSensor(LEGACY_ORIENTATION_SENSOR_TYPE);
    }

    @Nullable
    static Sensor defaultSensor(@Nullable SensorManager sensorManager, int appSensorType) {
        if (sensorManager == null || appSensorType == HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE) {
            return null;
        }
        return sensorManager.getDefaultSensor(toAndroidSensorType(appSensorType));
    }

    static boolean matchesSelectedSensor(@Nullable Sensor selectedSensor, int eventSensorType) {
        return selectedSensor != null && selectedSensor.getType() == eventSensorType;
    }

    static boolean matchesLegacyOrientationSensor(
            @Nullable Sensor legacyOrientationSensor,
            int eventSensorType
    ) {
        return legacyOrientationSensor != null
                && eventSensorType == LEGACY_ORIENTATION_SENSOR_TYPE
                && legacyOrientationSensor.getType() == eventSensorType;
    }

    static int appSensorType(@Nullable Sensor sensor) {
        if (sensor == null) {
            return HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE;
        }
        switch (sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                return HeadingSensorSupport.SENSOR_TYPE_ROTATION_VECTOR;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return HeadingSensorSupport.SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
            case LEGACY_ORIENTATION_SENSOR_TYPE:
                return HeadingSensorSupport.SENSOR_TYPE_LEGACY_ORIENTATION;
            default:
                return HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE;
        }
    }

    @NonNull
    static String describeSensor(@Nullable Sensor sensor) {
        int appSensorType = appSensorType(sensor);
        if (appSensorType != HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE) {
            return HeadingSensorSupport.describeSensorType(appSensorType);
        }
        return sensor == null ? "unavailable" : sensor.getStringType();
    }

    static int toHeadingAccuracyStatus(int androidAccuracy) {
        switch (androidAccuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return HeadingAccuracyStatus.HIGH;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return HeadingAccuracyStatus.MEDIUM;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return HeadingAccuracyStatus.LOW;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
            default:
                return HeadingAccuracyStatus.UNRELIABLE;
        }
    }

    private static int toAndroidSensorType(int appSensorType) {
        switch (appSensorType) {
            case HeadingSensorSupport.SENSOR_TYPE_ROTATION_VECTOR:
                return Sensor.TYPE_ROTATION_VECTOR;
            case HeadingSensorSupport.SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR;
            case HeadingSensorSupport.SENSOR_TYPE_LEGACY_ORIENTATION:
                return LEGACY_ORIENTATION_SENSOR_TYPE;
            case HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE:
            default:
                throw new IllegalArgumentException("Unsupported heading sensor type=" + appSensorType);
        }
    }
}
