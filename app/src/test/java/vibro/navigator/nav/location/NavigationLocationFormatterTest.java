package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationLocationFormatterTest {
    @Test
    public void absentAccuracyIsExplicitlyUnknownAndAbsentLocationIsNull() {
        String text = NavigationLocationFormatter.format(new NavigationLocation("gps"));
        assertTrue(text.contains("speedAccuracy=unknown"));
        assertTrue(text.contains("bearingAccuracy=unknown"));
        assertEquals("null", NavigationLocationFormatter.format(null));
    }

    @Test
    public void numericAccuracyIncludingZeroRemainsDistinctFromMissingAccuracy() {
        NavigationLocation location = new NavigationLocation("gps");
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        location.setBearingAccuracyDegrees(0f);
        String text = NavigationLocationFormatter.format(location);
        assertTrue(text.contains("speedAccuracy=0.1"));
        assertTrue(text.contains("bearingAccuracy=0.0"));
    }
}
