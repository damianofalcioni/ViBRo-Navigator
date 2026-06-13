package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
public class NavigationCompassRouteStartApproachRendererTest {

    @Test
    public void drawTargetLine_usesDashedPathInsteadOfRawLine() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassRouteStartApproachRenderer renderer = new NavigationCompassRouteStartApproachRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888));

        renderer.drawTargetLineForTest(canvas, activity, 100f, 100f, 160f, 100f);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        assertEquals(1, shadowCanvas.getPathPaintHistoryCount());
        assertEquals(0, shadowCanvas.getLinePaintHistoryCount());
        assertTrue(shadowCanvas.getDrawnPathPaint(0).getPathEffect() instanceof DashPathEffect);
    }
}
