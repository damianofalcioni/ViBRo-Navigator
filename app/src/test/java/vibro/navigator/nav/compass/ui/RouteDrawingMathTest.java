package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RouteDrawingMathTest {
    @Test
    public void clipSegmentToBoundsKeepsInsideSegmentUnchanged() {
        RouteDrawingMath.ClippedSegment out = new RouteDrawingMath.ClippedSegment();

        assertTrue(RouteDrawingMath.clipSegmentToBounds(-20f, -10f, 30f, 15f, 100f, out));

        assertEquals(-20f, out.startX, 0.01f);
        assertEquals(-10f, out.startY, 0.01f);
        assertEquals(30f, out.endX, 0.01f);
        assertEquals(15f, out.endY, 0.01f);
    }

    @Test
    public void clipSegmentToBoundsPreservesSlopeForDistantCrossingSegment() {
        RouteDrawingMath.ClippedSegment out = new RouteDrawingMath.ClippedSegment();

        assertTrue(RouteDrawingMath.clipSegmentToBounds(-1_000f, -100f, 1_000f, 100f, 114f, out));

        assertEquals(-114f, out.startX, 0.01f);
        assertEquals(-11.4f, out.startY, 0.01f);
        assertEquals(114f, out.endX, 0.01f);
        assertEquals(11.4f, out.endY, 0.01f);
    }

    @Test
    public void clipSegmentToBoundsRejectsSegmentOutsideBounds() {
        RouteDrawingMath.ClippedSegment out = new RouteDrawingMath.ClippedSegment();

        assertFalse(RouteDrawingMath.clipSegmentToBounds(200f, -100f, 200f, 100f, 114f, out));
    }
}
