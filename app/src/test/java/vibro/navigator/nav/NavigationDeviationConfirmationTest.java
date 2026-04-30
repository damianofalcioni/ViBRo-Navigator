package vibro.navigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationDeviationConfirmationTest {

    private final RouteDeviationPolicy policy = new RouteDeviationPolicy();
    private final NavigationDeviationConfirmation confirmation = new NavigationDeviationConfirmation();

    @Test
    public void isConfirmed_requiresSecondSampleForMarginalOffTrackDecision() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(20.0, 10f, null, 90.0);

        assertFalse(confirmation.isConfirmed(decision, 1f));
        assertEquals(1, confirmation.pendingSampleCount());
        assertTrue(confirmation.isConfirmed(decision, 1f));
        assertEquals(2, confirmation.pendingSampleCount());
    }

    @Test
    public void isConfirmed_confirmsLargeOffTrackDecisionImmediately() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(31.0, 10f, null, 90.0);

        assertTrue(confirmation.isConfirmed(decision, 1f));
        assertEquals(0, confirmation.pendingSampleCount());
    }

    @Test
    public void isConfirmed_usesSmallerImmediateMarginAtHigherSpeed() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(24.0, 10f, null, 90.0);

        assertFalse(confirmation.isConfirmed(decision, 1f));
        confirmation.clear();

        assertTrue(confirmation.isConfirmed(decision, 8f));
    }

    @Test
    public void clear_resetsPendingSamples() {
        RouteDeviationPolicy.Decision decision = policy.evaluate(5.0, 5f, 180.0, 90.0);

        assertFalse(confirmation.isConfirmed(decision, 1f));
        confirmation.clear();

        assertEquals(0, confirmation.pendingSampleCount());
        assertFalse(confirmation.isConfirmed(decision, 1f));
        assertEquals(1, confirmation.pendingSampleCount());
    }
}
