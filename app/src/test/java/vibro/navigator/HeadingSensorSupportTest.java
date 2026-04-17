package vibro.navigator;

import static org.junit.Assert.assertEquals;

import android.hardware.Sensor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HeadingSensorSupportTest {

    @Test
    public void chooseBestSensorType_prefersGeomagneticRotationVector() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(true, true);

        assertEquals(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, sensorType);
    }

    @Test
    public void chooseBestSensorType_fallsBackToRotationVector() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(false, true);

        assertEquals(Sensor.TYPE_ROTATION_VECTOR, sensorType);
    }

    @Test
    public void chooseBestSensorType_reportsUnavailableWhenNoHeadingSensorExists() {
        int sensorType = HeadingSensorSupport.chooseBestSensorType(false, false);

        assertEquals(HeadingSensorSupport.SENSOR_TYPE_UNAVAILABLE, sensorType);
    }
}
