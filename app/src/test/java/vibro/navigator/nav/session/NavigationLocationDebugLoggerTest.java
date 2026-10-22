package vibro.navigator.nav.session;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;

import static org.junit.Assert.assertEquals;

public class NavigationLocationDebugLoggerTest {
    @Test
    public void distinguishesSelectionPreferenceFromDuplicateDispatch() {
        NavigationLocation gps = location("gps", 10_000L, 24f);
        NavigationLocation network = location("network", 10_500L, 20f);
        assertEquals("accuracy_improvement_below_switch_margin",
                NavigationLocationDebugLogger.selectionReason(network, gps, 11_000L));
        assertEquals("incoming_selected_dispatch_blocked",
                NavigationLocationDebugLogger.selectionReason(network, network, 11_000L));
        assertEquals("stale_incoming",
                NavigationLocationDebugLogger.selectionReason(network, gps, 30_000L));
    }

    private static NavigationLocation location(String provider, long time, float accuracy) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setTime(time, time);
        location.setAccuracy(accuracy);
        return location;
    }
}
