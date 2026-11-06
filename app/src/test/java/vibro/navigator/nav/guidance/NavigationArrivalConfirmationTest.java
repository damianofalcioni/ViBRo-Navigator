package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class NavigationArrivalConfirmationTest {
    private final LatLon target = new LatLon(0, 0);
    private final NavigationArrivalConfirmation confirmation = new NavigationArrivalConfirmation();

    @Test
    public void accurateIndependentObservationsConfirmButClusteredOrDuplicateFixesDoNot() {
        assertFalse(observe(fix(1_000, 5, 5), 5, target));
        assertFalse(observe(fix(1_000, 5, 5), 5, target));
        assertFalse(observe(fix(2_000, 4, 5), 5, target));
        assertTrue(observe(fix(4_000, 3, 5), 5, target));
    }

    @Test
    public void poorAccuracyCannotExpandDefinitiveArrivalToSixtySevenMeters() {
        assertFalse(observe(fix(1_000, 67, 54), 54, target));
        assertFalse(observe(fix(4_000, 65, 54), 54, target));
        assertEquals(20, NavigationArrivalConfirmation.radiusMeters(100), 0);
    }

    @Test
    public void invalidFixLongGapAndProviderChangeRestartConfirmation() {
        assertFalse(observe(fix(1_000, 5, 5), 5, target));
        assertFalse(observe(fix(4_000, 5, Float.NaN), 5, target));
        assertFalse(observe(fix(7_000, 5, 5), 5, target));
        assertFalse(observe(fix(20_000, 5, 5), 5, target));
        NavigationLocation network = fix(23_000, 5, 5);
        network.setProvider("network");
        assertFalse(observe(network, 5, target));
        network.setTime(26_000, 26_000);
        assertTrue(observe(network, 5, target));
    }

    @Test
    public void targetChangeAndExplicitResetCannotReuseEarlierArrival() {
        observe(fix(1_000, 5, 5), 5, target);
        assertTrue(observe(fix(4_000, 4, 5), 5, target));
        assertFalse(observe(fix(7_000, 4, 5), 5, new LatLon(0.001, 0)));
        confirmation.reset();
        assertFalse(observe(fix(10_000, 4, 5), 5, target));
    }

    @Test
    public void staleAndFutureEvidenceResetArrivalAndRequireFreshConfirmation() {
        assertFalse(confirmation.observe(fix(1_000, 0, 5), 5, target, 15_000));
        assertFalse(confirmation.observe(fix(4_000, 0, 5), 5, target, 18_000));
        assertFalse(confirmation.observe(fix(19_000, 0, 5), 5, target, 19_000));
        assertFalse(confirmation.observe(fix(22_000, 0, 5), 5, target, 21_000));
        assertFalse(confirmation.observe(fix(22_000, 0, 5), 5, target, 22_000));
        assertTrue(confirmation.observe(fix(25_000, 0, 5), 5, target, 25_000));
        assertFalse(confirmation.observe(fix(25_000, 0, 5), 5, target, 35_001));
        assertFalse(confirmation.observe(fix(36_000, 0, 5), 5, target, 36_000));
    }

    @Test
    public void ordinaryDeliveryDelayAndTheFreshnessBoundaryStillAllowArrival() {
        assertFalse(confirmation.observe(fix(1_000, 0, 5), 5, target, 11_000));
        assertTrue(confirmation.observe(fix(4_000, 0, 5), 5, target, 14_000));
    }

    private boolean observe(NavigationLocation location, float accuracy, LatLon destination) {
        return confirmation.observe(location, accuracy, destination, location.getElapsedRealtimeOrTimeMs());
    }

    private static NavigationLocation fix(long time, double north, float accuracy) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(time, time);
        fix.setLatitude(north / 111_320);
        fix.setAccuracy(accuracy);
        return fix;
    }
}
