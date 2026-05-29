package vibro.navigator.sensor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HeadingSensorSupportTest {

    @Test
    public void chooseBestSensorType_prefersRotationVector() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(true, true);

        assertEquals(HeadingSensorSupport.SENSOR_TYPE_ROTATION_VECTOR, sensorType);
    }

    @Test
    public void chooseBestSensorType_fallsBackToGeomagneticRotationVector() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(true, false);

        assertEquals(HeadingSensorSupport.SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR, sensorType);
    }

    @Test
    public void chooseBestSensorType_reportsUnavailableWhenNoHeadingSensorExists() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(false, false);

        assertEquals(HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE, sensorType);
    }
}
