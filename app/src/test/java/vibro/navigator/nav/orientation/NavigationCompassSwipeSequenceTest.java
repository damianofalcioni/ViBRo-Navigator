package vibro.navigator.nav.orientation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationCompassSwipeSequenceTest {
    private final NavigationCompassSwipeSequence swipes = new NavigationCompassSwipeSequence(48f);

    @Test
    public void horizontalTravelSelectsDirectionOnlyAtRelease() {
        swipes.down(200f, 100f);
        assertFalse(swipes.move(153f, 100f));
        assertTrue(swipes.move(152f, 100f));
        assertEquals(1, swipes.up(152f, 100f));
        assertEquals(0, swipes.up(100f, 100f));
        swipes.down(100f, 100f);
        assertEquals(-1, swipes.up(200f, 100f));
    }

    @Test
    public void tapsShortDragsAndReturnToOriginDoNotSwipe() {
        swipes.down(100f, 100f);
        assertEquals(0, swipes.up(100f, 100f));
        swipes.down(100f, 100f);
        assertEquals(0, swipes.up(147f, 100f));
        swipes.down(100f, 100f);
        assertTrue(swipes.move(200f, 100f));
        assertEquals(0, swipes.up(100f, 100f));
    }

    @Test
    public void diagonalTravelMustBePredominantlyHorizontal() {
        swipes.down(100f, 100f);
        assertEquals(0, swipes.up(200f, 151f));
        swipes.down(100f, 100f);
        assertEquals(-1, swipes.up(200f, 150f));
    }

    @Test
    public void verticalDragCannotBecomeASwipeLater() {
        swipes.down(100f, 100f);
        assertFalse(swipes.move(100f, 200f));
        assertEquals(0, swipes.up(250f, 100f));
    }

    @Test
    public void cancellationAndNewDownResetTracking() {
        swipes.down(100f, 100f);
        swipes.cancel();
        assertEquals(0, swipes.up(200f, 100f));
        swipes.down(100f, 100f);
        assertEquals(-1, swipes.up(200f, 100f));
    }
}
