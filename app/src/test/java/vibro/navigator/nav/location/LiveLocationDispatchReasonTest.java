package vibro.navigator.nav.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LiveLocationDispatchReasonTest {
    private static final String GPS = "gps";
    private static final String NETWORK = "network";
    @Test
    public void distinguishesChronologyAccuracyAndCadenceRejections() {
        NavigationLocationFix gps = fix(GPS, 100_000, 20);
        assertEquals("older_than_last_accepted", reason(gps, fix(NETWORK, 99_000, 5), 60_000));
        assertEquals("same_measurement_time_without_accuracy_improvement", reason(gps, gps, 60_000));
        assertEquals("accuracy_worse_within_time_tolerance", reason(gps, fix(GPS, 100_500, 50), 60_000));
        assertEquals("cross_provider_requested_interval_not_elapsed", reason(gps, fix(NETWORK, 101_000, 20), 1_000));
        assertNull(reason(gps, fix(NETWORK, 101_000, 5), 1_000));
        assertNull(reason(gps, fix(NETWORK, 101_000, 20), 60_000));
        assertEquals("invalid_fix", reason(gps, fix(NETWORK, 101_000, Float.NaN), 60_000));
    }

    private static String reason(NavigationLocationFix previous, NavigationLocationFix candidate, long elapsedMs) {
        return LiveLocationDispatchPolicy.dispatchRejectionReason(previous, candidate, elapsedMs, 60_000);
    }

    private static NavigationLocationFix fix(String provider, long time, float accuracy) {
        return new NavigationLocationFix(provider, time, accuracy, 48.2, 16.37);
    }
}
