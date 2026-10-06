package vibro.navigator.nav.orientation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationCompassTapSequenceTest {
    private final NavigationCompassTapSequence taps = new NavigationCompassTapSequence(8f, 100f, 300L, 500L);

    @Test
    public void twoQuickTapsSwitchOnlyOnSecondRelease() {
        assertFalse(tap(100f, 0L));
        taps.down(102f, 100f, 100L);
        assertTrue(taps.isActive());
        assertTrue(taps.up(104f, 100f, 120L));
        assertFalse(taps.isActive());
    }

    @Test
    public void widelySeparatedOrSlowTapsStartNewPairs() {
        assertFalse(tap(100f, 0L));
        assertFalse(tap(201f, 100L));
        assertFalse(tap(201f, 421L));
        assertTrue(tap(201f, 500L));
    }

    @Test
    public void eachPairProducesOneSwitch() {
        assertFalse(tap(100f, 0L));
        assertTrue(tap(100f, 100L));
        assertFalse(tap(100f, 200L));
        assertTrue(tap(100f, 300L));
    }

    @Test
    public void dragReturningToOriginStillCancelsPendingPair() {
        tap(100f, 0L);
        taps.down(100f, 100f, 100L);
        taps.move(109f, 100f);
        taps.move(100f, 100f);
        assertFalse(taps.up(100f, 100f, 120L));
        assertFalse(tap(100f, 150L));
        assertTrue(tap(100f, 200L));
    }

    @Test
    public void movementOnReleaseCancelsPair() {
        tap(100f, 0L);
        taps.down(100f, 100f, 100L);
        assertFalse(taps.up(100f, 109f, 120L));
        assertFalse(tap(100f, 150L));
    }

    @Test
    public void longPressCannotCompleteDoubleTap() {
        tap(100f, 0L);
        taps.down(100f, 100f, 100L);
        assertFalse(taps.up(100f, 100f, 600L));
        assertFalse(tap(100f, 650L));
    }

    @Test
    public void cancellationClearsBothTouchesAndPendingTap() {
        tap(100f, 0L);
        taps.down(100f, 100f, 100L);
        taps.cancel();
        assertFalse(taps.up(100f, 100f, 120L));
        assertFalse(tap(100f, 150L));
    }

    @Test
    public void duplicateReleasesAndBackwardsTimeCannotSwitch() {
        assertFalse(taps.up(100f, 100f, 0L));
        tap(100f, 100L);
        assertFalse(taps.up(100f, 100f, 130L));
        taps.down(100f, 100f, 200L);
        assertFalse(taps.up(100f, 100f, 199L));
        assertFalse(tap(100f, 250L));
    }

    private boolean tap(float x, long time) {
        taps.down(x, 100f, time);
        return taps.up(x, 100f, time + 20L);
    }
}
