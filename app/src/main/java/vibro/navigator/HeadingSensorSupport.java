package vibro.navigator;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class HeadingSensorSupport {

    public static final int SENSOR_TYPE_UNAVAILABLE = -1;

    private HeadingSensorSupport() {
    }

    public static int chooseBestSensorType(boolean hasGeomagneticRotationVector, boolean hasRotationVector) {
        if (hasGeomagneticRotationVector) {
            return Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR;
        }
        if (hasRotationVector) {
            return Sensor.TYPE_ROTATION_VECTOR;
        }
        return SENSOR_TYPE_UNAVAILABLE;
    }

    @Nullable
    public static Sensor findBestSensor(@Nullable SensorManager sensorManager) {
        if (sensorManager == null) {
            return null;
        }
        int sensorType = chooseBestSensorType(
                sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        );
        if (sensorType == SENSOR_TYPE_UNAVAILABLE) {
            return null;
        }
        return sensorManager.getDefaultSensor(sensorType);
    }

    public static boolean matchesSelectedSensor(@Nullable Sensor selectedSensor, int eventSensorType) {
        return selectedSensor != null && selectedSensor.getType() == eventSensorType;
    }

    @StringRes
    public static int labelResIdForSensor(@Nullable Sensor sensor) {
        if (sensor != null && sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            return R.string.label_sensor_rotation_vector;
        }
        return R.string.label_sensor_geomagnetic_rotation_vector;
    }

    public static String describeSensor(@Nullable Sensor sensor) {
        if (sensor == null) {
            return "unavailable";
        }
        if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            return "rotation vector";
        }
        if (sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            return "geomagnetic rotation vector";
        }
        return sensor.getStringType();
    }
}
