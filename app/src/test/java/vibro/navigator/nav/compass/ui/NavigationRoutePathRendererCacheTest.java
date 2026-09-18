package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationRoutePathRendererCacheTest {
    @Test
    public void reusesProjectedPathUntilHeadingOrSourceChanges() {
        NavigationRoutePathRenderer renderer = new NavigationRoutePathRenderer();
        Canvas canvas = new Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
        Paint paint = new Paint();
        Object source = new Object();
        int[] projectionCount = {0};
        NavigationRoutePathRenderer.ProjectedRoutePointSource points = (index, out) -> {
            projectionCount[0]++;
            out.set(0f, index * 50f);
            return true;
        };

        draw(renderer, canvas, paint, source, 0f, points);
        draw(renderer, canvas, paint, source, 0f, points);
        assertEquals(2, projectionCount[0]);

        draw(renderer, canvas, paint, source, 90f, points);
        assertEquals(4, projectionCount[0]);
        draw(renderer, canvas, paint, new Object(), 90f, points);
        assertEquals(6, projectionCount[0]);
    }

    private static void draw(
            NavigationRoutePathRenderer renderer,
            Canvas canvas,
            Paint paint,
            Object source,
            float headingDegrees,
            NavigationRoutePathRenderer.ProjectedRoutePointSource points
    ) {
        renderer.drawProjectedRouteSegment(
                canvas, 50f, 50f, 1f, 0, 2, 100f, 0f,
                source, 0, headingDegrees, paint, points
        );
    }
}
