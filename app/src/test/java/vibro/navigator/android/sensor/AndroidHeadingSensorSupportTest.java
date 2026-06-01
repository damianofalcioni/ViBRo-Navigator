package vibro.navigator.android.sensor;

import static org.junit.Assert.assertEquals;

import android.hardware.SensorManager;

import org.junit.Test;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

public class AndroidHeadingSensorSupportTest {
    @Test
    public void toHeadingAccuracyStatus_mapsAndroidAccuracyConstants() {
        assertEquals(
                HeadingAccuracyStatus.HIGH,
                AndroidHeadingSensorSupport.toHeadingAccuracyStatus(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        );
        assertEquals(
                HeadingAccuracyStatus.MEDIUM,
                AndroidHeadingSensorSupport.toHeadingAccuracyStatus(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
        );
        assertEquals(
                HeadingAccuracyStatus.LOW,
                AndroidHeadingSensorSupport.toHeadingAccuracyStatus(SensorManager.SENSOR_STATUS_ACCURACY_LOW)
        );
        assertEquals(
                HeadingAccuracyStatus.UNRELIABLE,
                AndroidHeadingSensorSupport.toHeadingAccuracyStatus(SensorManager.SENSOR_STATUS_UNRELIABLE)
        );
    }

    @Test
    public void toHeadingAccuracyStatus_treatsUnknownStatusAsUnreliable() {
        assertEquals(
                HeadingAccuracyStatus.UNRELIABLE,
                AndroidHeadingSensorSupport.toHeadingAccuracyStatus(999)
        );
    }
}
