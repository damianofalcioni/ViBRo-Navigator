package vibro.navigator.about;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AboutSensorStatusFormatterTest {

    @Test
    public void describeLocationValue_includesExtendedGpsDetails() {
        long nowMs = 100_000L;
        AboutSensorValueFormatter.LocationSnapshot location =
                new AboutSensorValueFormatter.LocationSnapshot(
                        48.2082d,
                        16.3738d,
                        nowMs - 2_000L,
                        5f,
                        245.4d,
                        4.5f,
                        182.2f,
                        9.4f
                );

        String value = AboutSensorValueFormatter.describeLocationValue(location, 7, nowMs);

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
