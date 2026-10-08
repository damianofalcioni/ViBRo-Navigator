package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.nav.compass.CompassPerspectiveScale;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassCentralPerspectiveTest {
    @Test
    public void centralProjectionShrinksFarWidthsAndEnlargesNearWidthsAroundFixedOrigin() {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        assertTrue(projection.configure(150f, 150f, 600f, 1f, 30f, true));
        float[] point = new float[2];
        projection.mapPoint(150f, 150f, point);
        assertEquals(150f, point[0], 0.001f);
        assertEquals(180f, point[1], 0.001f);
        projection.mapPoint(250f, -50f, point);
        assertEquals(150f + 100f / 1.2f, point[0], 0.001f);
        assertEquals(180f - 116f / 1.2f, point[1], 0.001f);
        projection.mapPoint(250f, 350f, point);
        assertEquals(275f, point[0], 0.001f);
        assertEquals(325f, point[1], 0.001f);
    }

    @Test
    public void zeroTiltIsIdentityAndReturningToOrthographicRemovesDepth() {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        float[] point = new float[2];
        projection.configure(150f, 150f, 600f, 0f, 0f, true);
        projection.mapPoint(250f, 350f, point);
        assertEquals(250f, point[0], 0.001f);
        assertEquals(350f, point[1], 0.001f);
        projection.configure(150f, 150f, 600f, 1f, 0f, true);
        projection.configure(150f, 150f, 600f, 1f, 30f, false);
        projection.mapPoint(250f, 350f, point);
        assertEquals(250f, point[0], 0.001f);
        assertEquals(296f, point[1], 0.001f);
        assertFalse(projection.configure(150f, 150f, Float.NaN, 1f, 0f, true));
        assertFalse(projection.configure(150f, 150f, 0f, 1f, 0f, true));
    }

    @Test
    public void preparedPlaneCoversForwardEdgeAndStaysFiniteAtEveryInclination() {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        float[] point = new float[2];
        float preparedRadius = 100f * CompassPerspectiveScale.maximumViewportMultiplier(true);
        for (float progress : new float[] {0f, 0.25f, 0.5f, 1f, 1.25f}) {
            float offset = 32f * progress;
            projection.configure(150f, 150f, preparedRadius, progress, offset, true);
            projection.mapPoint(150f, 150f - preparedRadius, point);
            assertTrue("Prepared forward edge must cover the dial", point[1] <= 50f);
            projection.mapPoint(150f,
                    150f - 100f * CompassPerspectiveScale.viewportMultiplier(progress, true), point);
            assertEquals(50f + offset, point[1], 0.001f);
            projection.mapPoint(150f + preparedRadius, 150f + preparedRadius, point);
            assertTrue(Float.isFinite(point[0]));
            assertTrue(Float.isFinite(point[1]));
        }
    }

    @Test
    public void canvasDrawsTheSameProjectedPointsAndClipsBehindCameraGeometry() {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        projection.configure(150f, 150f, 600f, 1f, 30f, true);
        Bitmap bitmap = Bitmap.createBitmap(300, 350, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        int saved = canvas.save();
        projection.concat(canvas);
        canvas.drawRect(240f, -60f, 260f, -40f, paint);
        canvas.drawRect(240f, 340f, 260f, 360f, paint);
        canvas.restoreToCount(saved);
        assertEquals(Color.RED, bitmap.getPixel(233, 83));
        assertEquals(Color.RED, bitmap.getPixel(275, 325));
        bitmap.eraseColor(Color.TRANSPARENT);
        saved = canvas.save();
        projection.concat(canvas);
        canvas.drawRect(-2_000f, 800f, 2_000f, 2_000f, paint);
        canvas.restoreToCount(saved);
        assertTrue(bitmap.sameAs(Bitmap.createBitmap(300, 350, Bitmap.Config.ARGB_8888)));
    }
}
