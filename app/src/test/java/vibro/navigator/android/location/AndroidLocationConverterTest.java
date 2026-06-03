package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.location.NavigationLocation;

@RunWith(RobolectricTestRunner.class)
public class AndroidLocationConverterTest {
    private static final String GPS_PROVIDER = "gps";

    @Test
    public void toNavigationLocation_copiesCoreAndOptionalFields() {
        Location androidLocation = new Location(GPS_PROVIDER);
        androidLocation.setTime(1234L);
        androidLocation.setLatitude(48.2082);
        androidLocation.setLongitude(16.3738);
        androidLocation.setAccuracy(4.5f);
        androidLocation.setAltitude(188.0);
        androidLocation.setSpeed(1.25f);
        androidLocation.setBearing(91.0f);
        androidLocation.setBearingAccuracyDegrees(7.5f);

        NavigationLocation location = AndroidLocationConverter.toNavigationLocation(androidLocation);

        assertEquals(GPS_PROVIDER, location.getProvider());
        assertEquals(1234L, location.getTime());
        assertEquals(48.2082, location.getLatitude(), 0.0);
        assertEquals(16.3738, location.getLongitude(), 0.0);
        assertTrue(location.hasAccuracy());
        assertEquals(4.5f, location.getAccuracy(), 0.0f);
        assertTrue(location.hasAltitude());
        assertEquals(188.0, location.getAltitude(), 0.0);
        assertTrue(location.hasSpeed());
        assertEquals(1.25f, location.getSpeed(), 0.0f);
        assertTrue(location.hasBearing());
        assertEquals(91.0f, location.getBearing(), 0.0f);
        assertTrue(location.hasBearingAccuracy());
        assertEquals(7.5f, location.getBearingAccuracyDegrees(), 0.0f);
    }

    @Test
    public void toNavigationLocation_preservesMissingOptionalFields() {
        Location androidLocation = new Location("network");
        androidLocation.setLatitude(1.0);
        androidLocation.setLongitude(2.0);

        NavigationLocation location = AndroidLocationConverter.toNavigationLocation(androidLocation);

        assertEquals("network", location.getProvider());
        assertFalse(location.hasAccuracy());
        assertFalse(location.hasAltitude());
        assertFalse(location.hasSpeed());
        assertFalse(location.hasBearing());
        assertFalse(location.hasBearingAccuracy());
    }

    @Test
    public void toNavigationLocation_returnsNullForNullInput() {
        assertNull(AndroidLocationConverter.toNavigationLocation(null));
    }

}
