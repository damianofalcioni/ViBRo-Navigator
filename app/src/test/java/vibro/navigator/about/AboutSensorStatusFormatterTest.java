package vibro.navigator.about;

import static org.junit.Assert.assertTrue;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AboutSensorStatusFormatterTest {

    @Test
    public void describeLocationValue_includesExtendedGpsDetails() {
        long nowMs = System.currentTimeMillis();
        Location location = new Location("gps");
        location.setLatitude(48.2082d);
        location.setLongitude(16.3738d);
        location.setAccuracy(5f);
        location.setAltitude(245.4d);
        location.setSpeed(4.5f);
        location.setBearing(182.2f);
        location.setBearingAccuracyDegrees(9.4f);
        location.setTime(nowMs - 2_000L);

        String value = AboutSensorValueFormatter.describeLocationValue(location, 7);

        assertTrue(value.contains("lat=48.208200 lon=16.373800"));
        assertTrue(value.contains("acc=5.0m"));
        assertTrue(value.contains("alt=245.4m"));
        assertTrue(value.contains("speed=16.2km/h"));
        assertTrue(value.contains("bearing=182deg"));
        assertTrue(value.contains("bearingAcc=9deg"));
        assertTrue(value.contains("sats=7"));
        assertTrue(value.contains("age="));
    }
}
