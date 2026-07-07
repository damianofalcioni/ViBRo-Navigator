package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.TypedValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowCanvas;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassHeadingGuideGeometryTest {
    @Test
    public void arrowHeadMatchesOrientationCueMarkerGeometry() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView compassView = new NavigationCompassView(activity);
        int sizePx = 300;
        Canvas canvas = new Canvas(Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888));
        compassView.layout(0, 0, sizePx, sizePx);

        compassView.draw(canvas);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        int lineCount = shadowCanvas.getLinePaintHistoryCount();
        ShadowCanvas.LinePaintHistoryEvent leftEdge = shadowCanvas.getDrawnLine(lineCount - 2);
        ShadowCanvas.LinePaintHistoryEvent rightEdge = shadowCanvas.getDrawnLine(lineCount - 1);
        float center = sizePx / 2f;
        float tipY = dp(activity, 10f);
        float baseY = tipY + dp(activity, NavigationCompassOrientationCueRenderer.MARKER_HEIGHT_DP);
        float halfWidth = dp(activity, NavigationCompassOrientationCueRenderer.MARKER_WIDTH_DP) / 2f;

        assertEquals(1f, NavigationCompassView.HEADING_GUIDE_ARROW_TIP_SCALE, 0.01f);
        assertEdge(leftEdge, center, tipY, center - halfWidth, baseY);
        assertEdge(rightEdge, center, tipY, center + halfWidth, baseY);
    }

    private static void assertEdge(
            ShadowCanvas.LinePaintHistoryEvent edge,
            float startX,
            float startY,
            float stopX,
            float stopY
    ) {
        assertEquals(startX, edge.startX, 0.01f);
        assertEquals(startY, edge.startY, 0.01f);
        assertEquals(stopX, edge.stopX, 0.01f);
        assertEquals(stopY, edge.stopY, 0.01f);
    }

    private static float dp(Activity activity, float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}
