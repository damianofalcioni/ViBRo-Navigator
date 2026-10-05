package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassFullscreenRingTest {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void portraitCircleFollowsPerspectiveThroughoutTiltInBothThemes() {
        assertRingPixels(300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void landscapeCircleFollowsPerspectiveThroughoutTiltInBothThemes() {
        assertRingPixels(500, 300, false);
    }

    @Test
    public void portraitLabelsFollowProjectedCircleIntersection() {
        assertProjectedLabels(300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeLabelsFollowProjectedCircleIntersection() {
        assertProjectedLabels(500, 300, false);
    }

    private static void assertRingPixels(int width, int height, boolean portrait) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (int theme : new int[] {R.style.Theme_ViBRoNavigator, R.style.Theme_ViBRoNavigator_Light}) {
            activity.setTheme(theme);
            for (float progress : new float[] {0f, 0.25f, 0.5f, 1f}) {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                view(activity, width, height, progress).draw(new Canvas(bitmap));
                float cy = height - 88f;
                float radius = headingRadius(width, height, portrait)
                        * CompassPerspectiveScale.viewportMultiplier(progress) * 0.91f;
                float[] point = new float[2];
                projection(width, height, progress).mapPoint(width / 2f + radius * 0.5f,
                        cy - radius * (float) Math.sin(Math.toRadians(60f)), point);
                Bitmap reference = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas referenceCanvas = new Canvas(reference);
                projection(width, height, progress).concat(referenceCanvas);
                Paint referencePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                referencePaint.setStyle(Paint.Style.STROKE);
                referencePaint.setStrokeWidth(2f);
                referencePaint.setColor(AndroidAppTheme.color(activity, R.attr.vibroCompassRingColor));
                referenceCanvas.drawCircle(width / 2f, cy, radius, referencePaint);
                assertTrue("Circle follows tilt", Color.alpha(strongestPixel(bitmap, point)) > 40);
                // Compare rendered opacity too: tilted, antialiased strokes need not contain opaque pixels.
                assertEquals(strongestPixel(reference, point), strongestPixel(bitmap, point));
            }
        }
    }

    private static void assertProjectedLabels(int width, int height, boolean portrait) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (float progress : new float[] {0f, 0.25f, 0.5f, 1f}) {
            Canvas canvas = new Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
            NavigationCompassView view = view(activity, width, height, progress);
            view.setCompassState(NavCompassState.fromProjectedPoints(
                    0f, null, 1f, 80f, 0f,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    0f, 0f, false));
            view.draw(canvas);
            ShadowCanvas drawing = Shadows.shadowOf(canvas);
            assertEquals(!portrait && progress == 0f ? 6 : 2, drawing.getTextHistoryCount());
            float radius = headingRadius(width, height, portrait)
                    * CompassPerspectiveScale.viewportMultiplier(progress) * 0.91f;
            for (int index = 0; index < 2; index++) {
                float side = index == 0 ? 1f : -1f;
                float[] anchor = new float[2];
                projection(width, height, progress).mapPoint(width / 2f + side * 3f,
                        height - 88f - radius, anchor);
                ShadowCanvas.TextHistoryEvent label = drawing.getDrawnTextEvent(index);
                float baseline = -label.paint.descent() - 6f;
                assertEquals(anchor[0] + side * 6f, label.x, 0.01f);
                assertEquals(anchor[1] + baseline, label.y, 0.01f);
            }
        }
    }

    private static NavigationCompassView view(Activity activity, int width, int height, float progress) {
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(true);
        view.setPerspectiveProgress(progress);
        view.layout(0, 0, width, height);
        return view;
    }

    private static float headingRadius(int width, int height, boolean portrait) {
        float cy = height - 88f;
        return portrait ? Math.min(width / 2f, cy) - 10f : cy - 16f;
    }

    private static NavigationCompassPerspective projection(int width, int height, float progress) {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        projection.configure(width / 2f, height - 88f,
                (height - 104f) * CompassPerspectiveScale.maximumViewportMultiplier(), progress);
        return projection;
    }

    private static int strongestPixel(Bitmap bitmap, float[] point) {
        int cx = Math.round(point[0]);
        int cy = Math.round(point[1]);
        int strongestPixel = Color.TRANSPARENT;
        for (int y = cy - 2; y <= cy + 2; y++) {
            for (int x = cx - 2; x <= cx + 2; x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) > Color.alpha(strongestPixel)) {
                    strongestPixel = pixel;
                }
            }
        }
        return strongestPixel;
    }
}
