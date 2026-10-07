package vibro.navigator.nav.session;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;

import static org.junit.Assert.*;

public class NavigationLocationFilterIntegrationTest {
    @Test
    public void rejectedOutlierDoesNotBlockFollowingGoodFixOrAdvanceHistory() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        assertFalse(state.onRawLocationChanged(fix(1000, 0), 1000, false, 1000).isDropped());
        assertTrue(state.onRawLocationChanged(fix(2000, 40), 2000, false, 1000).isDropped());
        NavigationSessionLocationState.Update next = state.onRawLocationChanged(fix(3000, 0), 3000, false, 1000);
        assertFalse(next.isDropped());
        assertEquals(16.37, next.getFilteredLocation().getLongitude(), 0.000001);
    }

    @Test
    public void invalidAndDelayedCallbacksLeaveAcceptedLocationIntact() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        state.onRawLocationChanged(fix(1000, 0), 1000, false, 1000);
        NavigationLocation invalid = fix(2000, 0);
        invalid.setAccuracy(Float.NaN);
        assertTrue(state.onRawLocationChanged(invalid, 2000, false, 1000).isDropped());
        assertFalse(state.onRawLocationChanged(fix(3000, 0), 3000, false, 1000).isDropped());
        assertTrue(state.onRawLocationChanged(fix(2500, 1), 3500, false, 1000).isDropped());
        assertEquals(3000, state.getLastFilteredLocation().getElapsedRealtimeMs());
    }

    @Test
    public void sixtySecondCadenceDoesNotReacquireOrInventMotionOnReturnToFastCadence() {
        NavigationSessionLocationState state = new NavigationSessionLocationState();
        NavigationLocation walking = fix(1000, 0);
        walking.setSpeed(1.4f);
        state.onRawLocationChanged(walking, 1000, false, 60000);
        walking = fix(61000, 84);
        walking.setSpeed(1.4f);
        assertFalse(state.onRawLocationChanged(walking, 61000, false, 60000).isReacquiringAfterLongGap());
        NavigationSessionLocationState.Update stop = state.onRawLocationChanged(fix(121000, 0), 121000, false, 60000);
        assertFalse(stop.isDropped());
        assertFalse(stop.isReacquiringAfterLongGap());
        for (long time = 124000; time <= 136000; time += 3000) {
            NavigationSessionLocationState.Update next = state.onRawLocationChanged(fix(time, 0), time, false, 3000);
            assertFalse(next.isDropped());
            assertEquals(16.37, next.getFilteredLocation().getLongitude(), 0.000001);
        }
    }

    private static NavigationLocation fix(long time, double east) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(48.2);
        location.setLongitude(16.37 + east / (111320 * Math.cos(Math.toRadians(48.2))));
        location.setTime(1700000000000L + time, time);
        location.setAccuracy(5);
        location.setSpeed(0);
        return location;
    }
}
