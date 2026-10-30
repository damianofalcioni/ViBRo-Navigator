package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationMotionModel;
import static org.junit.Assert.*;
import static vibro.navigator.nav.location.NavigationLocationProviders.GPS_PROVIDER;
import static vibro.navigator.nav.location.NavigationLocationProviders.NETWORK_PROVIDER;

public class NavigationSessionLocationReplacementTest {
    @Test
    public void acceptedImprovementUpdatesRetainedPositionForEitherProvider() {
        for (String provider : new String[] {GPS_PROVIDER, NETWORK_PROVIDER}) {
            NavigationSessionLocationState state = new NavigationSessionLocationState();
            state.setActiveProviders("gps+network");
            accept(state, fix(GPS_PROVIDER, 100_000, 0, 20), 100_000);
            NavigationSessionLocationState.Update replacement =
                    accept(state, fix(provider, 100_000, 5, 5), 100_100);
            assertRetained(state, replacement.getFilteredLocation());
            assertFalse(state.isMotionKnown(100_100));
            assertNull(state.trustedActualBearingDegreesForReroute(state.getLastFilteredLocation()));
        }
    }

    @Test
    public void replacementDoesNotSupplyASecondIndependentStopObservation() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        accept(state, fix(GPS_PROVIDER, 100_000, 0, 20), 100_000);
        accept(state, fix(GPS_PROVIDER, 100_000, 0, 5), 100_100);
        assertFalse(state.isMotionKnown(100_100));
        assertEquals(5, state.getLastFilteredLocation().getAccuracy(), 0);
        accept(state, fix(GPS_PROVIDER, 103_000, 0, 5), 103_000);
        assertTrue(state.isLikelyStationary(103_000));
    }

    @Test
    public void correctedObservationClearsOldStopAndCoordinateHistory() {
        for (String provider : new String[] {GPS_PROVIDER, NETWORK_PROVIDER}) {
            NavigationSessionLocationState state = new NavigationSessionLocationState();
            state.setActiveProviders("gps+network");
            accept(state, fix(GPS_PROVIDER, 97_000, 0, 20), 97_000);
            accept(state, fix(GPS_PROVIDER, 100_000, 0, 20), 100_000);
            assertTrue(state.isLikelyStationary(100_000));
            NavigationSessionLocationState.Update replacement =
                    accept(state, fix(provider, 100_000, 20, 5), 100_100);
            assertRetained(state, replacement.getFilteredLocation());
            assertFalse(state.isMotionKnown(100_100));
            assertNull(state.trustedActualBearingDegreesForReroute(state.getLastFilteredLocation()));
            accept(state, fix(provider, 103_000, 20, 5), 103_000);
            assertTrue(state.isLikelyStationary(103_000));
        }
    }

    @Test
    public void olderOrUnimprovedDuplicatesCannotReplaceTheRetainedPosition() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        NavigationLocation initial = accept(state, fix(GPS_PROVIDER, 100_000, 0, 20), 100_000).getFilteredLocation();
        assertTrue(state.onRawLocationChanged(fix(GPS_PROVIDER, 99_999, 5, 5), 100_100, false, 3_000).isDropped());
        assertTrue(state.onRawLocationChanged(fix(GPS_PROVIDER, 100_000, 5, 20), 100_100, false, 3_000).isDropped());
        assertRetained(state, initial);
    }

    @Test
    public void accuracyImprovementRestoresPositionAfterAnUnknownAccuracyObservation() {
        NavigationLocationMotionModel model = new NavigationLocationMotionModel();
        NavigationLocation unknown = new NavigationLocation(GPS_PROVIDER);
        unknown.setTime(100_000, 100_000);
        model.recordFilteredLocation(unknown);
        NavigationLocation replacement = fix(GPS_PROVIDER, 100_000, 5, 5);
        model.recordFilteredLocation(replacement);
        assertSame(replacement, model.getLastFilteredLocation());
        assertFalse(model.motionState(100_100).isKnown());
    }

    private static NavigationSessionLocationState.Update accept(
            NavigationSessionLocationState state, NavigationLocation location, long receiptMs) {
        NavigationSessionLocationState.Update update = state.onRawLocationChanged(location, receiptMs, false, 3_000);
        assertFalse(update.isDropped());
        return update;
    }

    private static void assertRetained(NavigationSessionLocationState state, NavigationLocation accepted) {
        NavigationLocation retained = state.getLastFilteredLocation();
        assertNotNull(retained);
        assertEquals(accepted.getProvider(), retained.getProvider());
        assertEquals(accepted.getLatitude(), retained.getLatitude(), 0);
        assertEquals(accepted.getLongitude(), retained.getLongitude(), 0);
        assertEquals(accepted.getAccuracy(), retained.getAccuracy(), 0);
        assertEquals(accepted.getElapsedRealtimeOrTimeMs(), retained.getElapsedRealtimeOrTimeMs());
    }

    private static NavigationLocation fix(String provider, long timeMs, double northMeters, float accuracy) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(1_700_000_000_000L + timeMs, timeMs);
        location.setLatitude(northMeters / 111_320.0);
        location.setAccuracy(accuracy);
        location.setSpeed(0);
        return location;
    }
}
