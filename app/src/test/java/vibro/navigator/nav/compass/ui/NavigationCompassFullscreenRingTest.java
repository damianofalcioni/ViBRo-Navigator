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
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.nav.format.NavigationTextFormatter;

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

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void portraitTiltLowersCenterWithoutLengtheningArrow() {
        assertArrowAndCenter(300, 500, true);
    }

    @Test
    @Config(qualifiers = "land")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void landscapeTiltLowersCenterWithoutLengtheningArrow() {
        assertArrowAndCenter(500, 300, false);
    }

    private static void assertArrowAndCenter(int width, int height, boolean portrait) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AppCompassSettings.setCentralPerspectiveEnabled(activity, false);
        float arrowLength = headingRadius(width, height, portrait);
        int previousCenterY = 0;
        for (float progress : new float[] {0f, 0.25f, 0.5f, 1f}) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            view(activity, width, height, progress).draw(new Canvas(bitmap));
            int centerY = Math.round(height - 88f + centerOffset(width, height, progress));
            assertEquals("Position marker follows the lowered route origin",
                    activity.getColor(R.color.compass_center), bitmap.getPixel(width / 2, centerY));
            int arrowTipY = 0;
            while (Color.alpha(bitmap.getPixel(width / 2, arrowTipY)) == 0) {
                arrowTipY++;
            }
            assertEquals("Screen arrow length stays constant throughout tilt", arrowLength,
                    centerY - arrowTipY, 2f);
            assertTrue("Center moves down as the view tilts", centerY > previousCenterY);
            previousCenterY = centerY;
        }
    }

    private static void assertRingPixels(int width, int height, boolean portrait) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AppCompassSettings.setCentralPerspectiveEnabled(activity, false);
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
        AppCompassSettings.setCentralPerspectiveEnabled(activity, false);
        for (float progress : new float[] {0f, 0.25f, 0.5f, 1f}) {
            Canvas canvas = new Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
            NavigationCompassView view = view(activity, width, height, progress);
            view.setCompassState(legendState(progress));
            view.draw(canvas);
            ShadowCanvas drawing = Shadows.shadowOf(canvas);
            assertEquals(!portrait && progress == 0f ? 6 : 2, drawing.getTextHistoryCount());
            float radius = headingRadius(width, height, portrait)
                    * CompassPerspectiveScale.viewportMultiplier(progress) * 0.91f;
            float distance = 80f * CompassPerspectiveScale.viewportMultiplier(progress) * 0.91f
                    * headingRadius(width, height, portrait) / (height - 104f);
            assertEquals(NavigationTextFormatter.formatDistance(activity, distance),
                    drawing.getDrawnTextEvent(0).text);
            assertEquals(NavigationTextFormatter.formatTimeSeconds(activity, Math.round(distance)),
                    drawing.getDrawnTextEvent(1).text);
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

    private static NavCompassState legendState(float progress) {
        float visibleRadius = progress == 0f ? 80f : 80f * CompassPerspectiveScale.maximumViewportMultiplier();
        return NavCompassState.fromProjectedPoints(
                0f, null, 1f, visibleRadius, 0f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                0f, 0f, false);
    }

    private static float headingRadius(int width, int height, boolean portrait) {
        float cy = height - 88f;
        return portrait ? Math.min(width / 2f, cy) - 10f : cy - 16f;
    }

    private static NavigationCompassPerspective projection(int width, int height, float progress) {
        NavigationCompassPerspective projection = new NavigationCompassPerspective();
        projection.configure(width / 2f, height - 88f,
                (height - 104f) * CompassPerspectiveScale.maximumViewportMultiplier(), progress,
                centerOffset(width, height, progress));
        return projection;
    }

    private static float centerOffset(int width, int height, float progress) {
        return (Math.min(width / 2f, height - 88f) - 10f) * 0.91f * 0.32f * progress;
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
