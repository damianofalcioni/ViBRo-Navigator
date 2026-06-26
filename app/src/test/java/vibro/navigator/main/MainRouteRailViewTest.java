package vibro.navigator.main;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

@RunWith(RobolectricTestRunner.class)
public class MainRouteRailViewTest {
    @Test
    public void straightLineModeUsesDottedRailLinePaint() {
        MainRouteRailView railView = railView();

        railView.setStraightLineMode(true);

        assertTrue(railView.linePaintForTest().getPathEffect() instanceof DashPathEffect);
    }

    @Test
    public void straightLineModeDrawsDottedRailAsPathInsteadOfRawLine() {
        MainRouteRailView railView = railView();
        railView.setStraightLineMode(true);
        Canvas canvas = new Canvas(Bitmap.createBitmap(80, 200, Bitmap.Config.ARGB_8888));

        railView.drawRailLineForTest(canvas, 20f, 160f);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        assertEquals(1, shadowCanvas.getPathPaintHistoryCount());
        assertEquals(0, shadowCanvas.getLinePaintHistoryCount());
        assertTrue(shadowCanvas.getDrawnPathPaint(0).getPathEffect() instanceof DashPathEffect);
    }

    @Test
    public void routeModeUsesSolidRailLinePaint() {
        MainRouteRailView railView = railView();
        railView.setStraightLineMode(true);

        railView.setStraightLineMode(false);

        assertNull(railView.linePaintForTest().getPathEffect());
    }

    @Test
    public void routeArrowKeepsFixedDistanceFromRailBottomInRouteAndStraightLineModes() {
        MainRouteRailView railView = railView();

        assertArrowTracksBottomWithFixedOffset(railView);

        railView.setStraightLineMode(true);

        assertArrowTracksBottomWithFixedOffset(railView);
    }

    private static void assertArrowTracksBottomWithFixedOffset(MainRouteRailView railView) {
        float destinationY = 20f;
        float firstRailBottomY = 240f;
        float secondRailBottomY = 320f;

        Float firstArrowY = railView.routeArrowCenterYForTest(firstRailBottomY, destinationY);
        Float secondArrowY = railView.routeArrowCenterYForTest(secondRailBottomY, destinationY);

        assertNotNull(firstArrowY);
        assertNotNull(secondArrowY);
        assertEquals(firstRailBottomY - firstArrowY, secondRailBottomY - secondArrowY, 0.01f);
        assertTrue(secondArrowY > firstArrowY);
    }

    private static MainRouteRailView railView() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        return new MainRouteRailView(activity);
    }
}
