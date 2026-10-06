package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViBRoAutoGestureSequenceTest {
    private final ViBRoAutoGestureSequence gestures = new ViBRoAutoGestureSequence(48f, 8f, 300L);

    @Test
    public void horizontalDeltasAccumulateAndChangeViewOnlyOncePerBurst() {
        assertEquals(0, gestures.scroll(20f, 0f, 1_000L));
        assertEquals(1, gestures.scroll(30f, 0f, 1_010L));
        assertEquals(0, gestures.scroll(100f, 0f, 1_020L));
        assertEquals(0, gestures.scroll(-200f, 0f, 1_030L));
        assertEquals(-1, gestures.scroll(-50f, 0f, 1_331L));
    }

    @Test
    public void shortAndDiagonalTravelDoNotCycleViews() {
        assertEquals(0, gestures.scroll(47f, 0f, 1_000L));
        assertEquals(0, gestures.scroll(50f, 30f, 2_000L));
        assertEquals(0f, gestures.tiltDistance(), 0f);
    }

    @Test
    public void verticalDragLocksTiltAndCannotBecomeSwipe() {
        assertEquals(0, gestures.scroll(1f, 20f, 1_000L));
        assertEquals(20f, gestures.tiltDistance(), 0f);
        assertEquals(0, gestures.scroll(100f, -5f, 1_010L));
        assertEquals(-5f, gestures.tiltDistance(), 0f);
    }

    @Test
    public void scaleDeltasAccumulateAndConsumeAtMostOneZoomStep() {
        assertEquals(0, gestures.scale(1.1f, 1_000L));
        assertEquals(0, gestures.scale(1.1f, 1_010L));
        assertEquals(1, gestures.scale(1.1f, 1_020L));
        assertEquals(0, gestures.scale(3f, 1_030L));
        assertEquals(0, gestures.scale(0.1f, 1_040L));
        assertEquals(-1, gestures.scale(0.5f, 1_341L));
    }

    @Test
    public void hostDoubleTapZoomFactorProducesOneStep() {
        assertEquals(1, gestures.scale(2f, 1_000L));
        assertEquals(0, gestures.scale(2f, 1_010L));
    }

    @Test
    public void scaleCancelsPendingSwipeAndBlocksTilt() {
        assertEquals(0, gestures.scroll(30f, 0f, 1_000L));
        assertEquals(1, gestures.scale(2f, 1_010L));
        assertEquals(0, gestures.scroll(100f, 100f, 1_020L));
        assertEquals(0f, gestures.tiltDistance(), 0f);
    }

    @Test
    public void nonFiniteDeltasAndInvalidScalesDoNotStartInputBurst() {
        assertEquals(0, gestures.scroll(Float.NaN, 0f, 1_000L));
        assertEquals(0, gestures.scroll(0f, Float.POSITIVE_INFINITY, 1_000L));
        assertEquals(0, gestures.scale(Float.NaN, 1_000L));
        assertEquals(0, gestures.scale(Float.POSITIVE_INFINITY, 1_000L));
        assertEquals(0, gestures.scale(0f, 1_000L));
        assertEquals(0, gestures.scale(-1f, 1_000L));
        assertFalse(gestures.isRecent(1_000L));
    }

    @Test
    public void recentInputSuppressesReleaseClicksAndClearResetsTheBurst() {
        gestures.scale(2f, 1_000L);
        assertTrue(gestures.isRecent(1_300L));
        assertFalse(gestures.isRecent(1_301L));
        gestures.clear();
        assertFalse(gestures.isRecent(1_000L));
        assertEquals(1, gestures.scale(2f, 1_001L));
    }

    @Test
    public void clockRollbackStartsFreshBurst() {
        assertEquals(1, gestures.scale(2f, 1_000L));
        assertEquals(-1, gestures.scale(0.5f, 900L));
    }
}
