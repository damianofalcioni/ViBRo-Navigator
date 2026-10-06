package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationRoutePathRendererCacheTest {
    @Test
    public void reusesNorthUpPathAcrossHeadingChangesUntilSourceChanges() {
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
        draw(renderer, canvas, paint, source, 359f, points);
        assertEquals(2, projectionCount[0]);
        draw(renderer, canvas, paint, new Object(), 90f, points);
        assertEquals(4, projectionCount[0]);
    }

    @Test
    public void diagonalRotationReachesFullscreenEdgesInFlatAndTiltedViews() {
        assertDiagonalVisible(0f);
        assertDiagonalVisible(1.25f);
    }

    private static void assertDiagonalVisible(float tilt) {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();
        perspective.configure(150f, 150f, 350f, tilt);
        perspective.concat(canvas);
        NavigationRoutePathRenderer renderer = new NavigationRoutePathRenderer();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        Object source = new Object();
        NavigationRoutePathRenderer.ProjectedRoutePointSource points = (index, out) -> {
            out.set(0f, index == 0 ? -1000f : 1000f);
            return true;
        };

        for (float heading : new float[]{45f, 135f, 225f, 315f}) {
            bitmap.eraseColor(Color.TRANSPARENT);
            renderer.drawProjectedRouteSegment(canvas, 150f, 150f, 1f, 0, 2,
                    100f, 0f, source, 0, heading, paint, points);
            NavigationRoutePathRenderer.PlotPoint projected = new NavigationRoutePathRenderer.PlotPoint();
            NavigationCompassRouteProjector.projectHeadingUp(0f, 127f, heading, projected);
            float[] screen = new float[2];
            perspective.mapPoint(150f + projected.x, 150f - projected.y, screen);
            assertEquals(Color.RED, bitmap.getPixel(Math.round(screen[0]), Math.round(screen[1])));
            // Rotated source bounds must not leak beyond the original heading-up drawing square.
            NavigationCompassRouteProjector.projectHeadingUp(0f, 160f, heading, projected);
            perspective.mapPoint(150f + projected.x, 150f - projected.y, screen);
            assertEquals(Color.TRANSPARENT, bitmap.getPixel(Math.round(screen[0]), Math.round(screen[1])));
        }
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
