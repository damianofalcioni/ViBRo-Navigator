package vibro.navigator.nav.compass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

public class CompassRadiusResolverTest {
    private static final String GPS_PROVIDER = "gps";

    @Test
    public void resolve_ignoresRawSpeedWhenResolvedSpeedIsZero() {
        NavigationLocation noisySpeedLocation = new NavigationLocation(GPS_PROVIDER);
        noisySpeedLocation.setLatitude(0.0);
        noisySpeedLocation.setLongitude(0.0);
        noisySpeedLocation.setSpeed(1.2f);

        CompassRadiusResolver.State state = CompassRadiusResolver.resolve(
                1_000.0,
                noisySpeedLocation,
                0f,
                false,
                null,
                null,
                0L,
                null,
                0L
        );

        assertFalse(state.usingMovingScale);
        assertTrue(state.visibleRadiusMeters > 1_000f);
    }
}
