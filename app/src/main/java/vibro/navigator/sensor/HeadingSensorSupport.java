package vibro.navigator.sensor;

import androidx.annotation.StringRes;

import vibro.navigator.R;

public final class HeadingSensorSupport {

    public static final int SENSOR_TYPE_UNAVAILABLE = -1;
    public static final int SENSOR_TYPE_ROTATION_VECTOR = 1;
    public static final int SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR = 2;
    public static final int SENSOR_TYPE_LEGACY_ORIENTATION = 3;

    private HeadingSensorSupport() {
    }

    public static int chooseBestSensorType(boolean hasGeomagneticRotationVector, boolean hasRotationVector) {
        if (hasRotationVector) {
            return SENSOR_TYPE_ROTATION_VECTOR;
        }
        if (hasGeomagneticRotationVector) {
            return SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
        }
        return SENSOR_TYPE_UNAVAILABLE;
    }

    @StringRes
    public static int labelResIdForSensorType(int sensorType) {
        if (sensorType == SENSOR_TYPE_ROTATION_VECTOR) {
            return R.string.label_sensor_rotation_vector;
        }
        if (sensorType == SENSOR_TYPE_LEGACY_ORIENTATION) {
            return R.string.label_sensor_orientation;
        }
        return R.string.label_sensor_geomagnetic_rotation_vector;
    }

    public static String describeSensorType(int sensorType) {
        if (sensorType == SENSOR_TYPE_UNAVAILABLE) {
            return "unavailable";
        }
        if (sensorType == SENSOR_TYPE_ROTATION_VECTOR) {
            return "rotation vector";
        }
        if (sensorType == SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            return "geomagnetic rotation vector";
        }
        if (sensorType == SENSOR_TYPE_LEGACY_ORIENTATION) {
            return "orientation";
        }
        return "unknown";
    }
}
