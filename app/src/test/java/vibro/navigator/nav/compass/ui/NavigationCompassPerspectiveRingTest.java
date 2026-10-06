package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.NavigationTextFormatter;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassPerspectiveRingTest {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void compactPerspectiveShowsOuterTravelRingAndLoweredPositionMarker() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        compactView(activity, 300, 60f * CompassPerspectiveScale.maximumViewportMultiplier(), 1f, null)
                .draw(new Canvas(bitmap));

        // The outer guide crosses this area inside the dial, away from the labels and arrow.
        assertTrue(hasRingPixels(bitmap, activity.getColor(R.color.compass_surface)));
        assertEquals(activity.getColor(R.color.compass_center), bitmap.getPixel(150, 191));
    }

    @Test
    public void compactPerspectiveLabelsUseExpandedHorizonThroughoutTilt() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (int horizon : new int[] {30, 45, 60}) {
            for (float progress : new float[] {0.25f, 0.5f, 1f}) {
                Canvas canvas = new Canvas(Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888));
                compactView(activity, 300, horizon * CompassPerspectiveScale.maximumViewportMultiplier(),
                        progress, null).draw(canvas);

                float expandedDistance = horizon * CompassPerspectiveScale.viewportMultiplier(progress);
                assertEquals(NavigationTextFormatter.formatTimeSeconds(activity, Math.round(expandedDistance)),
                        Shadows.shadowOf(canvas).getDrawnTextEvent(5).text);
                assertEquals(NavigationTextFormatter.formatDistance(activity, expandedDistance),
                        Shadows.shadowOf(canvas).getDrawnTextEvent(4).text);
                assertEquals(8, Shadows.shadowOf(canvas).getTextHistoryCount());
                assertEquals(NavigationTextFormatter.formatTimeSeconds(activity, Math.round(expandedDistance / 2f)),
                        Shadows.shadowOf(canvas).getDrawnTextEvent(7).text);
                assertEquals(NavigationTextFormatter.formatDistance(activity, expandedDistance / 2f),
                        Shadows.shadowOf(canvas).getDrawnTextEvent(6).text);
            }
        }
    }

    @Test
    public void perspectiveUsesProjected2dAnchorsWithUprightLabelsAndAccuracyArcs() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (Float accuracy : new Float[] {null, 20f, 85f}) {
            assertLegendMatches2d(activity, accuracy);
        }
    }

    private static void assertLegendMatches2d(Activity activity, Float accuracy) {
        float scale = CompassPerspectiveScale.viewportMultiplier(1f);
        int expandedSize = Math.round(2f * (140f * scale + 10f));
        Canvas perspectiveCanvas = new Canvas(Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888));
        Canvas flatCanvas = new Canvas(Bitmap.createBitmap(expandedSize, expandedSize, Bitmap.Config.ARGB_8888));
        compactView(activity, 300, 60f * CompassPerspectiveScale.maximumViewportMultiplier(), 1f, accuracy)
                .draw(perspectiveCanvas);
        compactView(activity, expandedSize, 60f * scale, 0f, accuracy).draw(flatCanvas);
        ShadowCanvas projected = Shadows.shadowOf(perspectiveCanvas);
        ShadowCanvas flat = Shadows.shadowOf(flatCanvas);
        NavigationCompassPerspective perspective = new NavigationCompassPerspective();
        perspective.configure(150f, 150f, 127.4f * CompassPerspectiveScale.maximumViewportMultiplier(),
                1f, 127.4f * 0.32f);
        assertEquals(8, flat.getTextHistoryCount());
        assertEquals(accuracy == null ? 0 : 2, flat.getArcPaintHistoryCount());
        assertEquals(flat.getTextHistoryCount(), projected.getTextHistoryCount());
        assertEquals(flat.getArcPaintHistoryCount(), projected.getArcPaintHistoryCount());
        for (int index = 4; index < flat.getTextHistoryCount(); index++) {
            assertProjectedLabel(flat.getDrawnTextEvent(index), projected.getDrawnTextEvent(index),
                    expandedSize / 2f, perspective);
        }
        assertAccuracyArcsMatch(flat, projected, expandedSize / 2f);
    }

    private static void assertProjectedLabel(
            ShadowCanvas.TextHistoryEvent flat, ShadowCanvas.TextHistoryEvent actual,
            float flatCenter, NavigationCompassPerspective perspective
    ) {
        Paint.FontMetrics metrics = flat.paint.getFontMetrics();
        float baselineOffset = -metrics.descent - 6f;
        float labelOffset = flat.paint.getTextAlign() == Paint.Align.LEFT ? 6f : -6f;
        float[] anchor = new float[2];
        perspective.mapPoint(flat.x - flatCenter + 150f - labelOffset,
                flat.y - flatCenter + 150f - baselineOffset, anchor);
        float width = flat.paint.measureText(flat.text);
        float minimumX = flat.paint.getTextAlign() == Paint.Align.RIGHT ? width : 0f;
        float maximumX = flat.paint.getTextAlign() == Paint.Align.LEFT ? 300f - width : 300f;
        assertEquals(flat.text, actual.text);
        assertEquals(flat.paint.getTextSize(), actual.paint.getTextSize(), 0.01f);
        assertEquals(Math.max(minimumX, Math.min(maximumX, anchor[0] + labelOffset)), actual.x, 1f);
        assertEquals(Math.max(-metrics.ascent, Math.min(300f - metrics.descent, anchor[1] + baselineOffset)),
                actual.y, 1f);
    }

    private static void assertAccuracyArcsMatch(ShadowCanvas flat, ShadowCanvas projected, float flatCenter) {
        for (int index = 0; index < flat.getArcPaintHistoryCount(); index++) {
            assertEquals(flat.getDrawnArc(index).startAngle, projected.getDrawnArc(index).startAngle, 0.01f);
            assertEquals(flat.getDrawnArc(index).sweepAngle, projected.getDrawnArc(index).sweepAngle, 0.01f);
            assertEquals(flat.getDrawnArc(index).oval.top - flatCenter,
                    projected.getDrawnArc(index).oval.top - 150f, 1f);
            assertEquals(flat.getDrawnArc(index).oval.right - flatCenter,
                    projected.getDrawnArc(index).oval.right - 150f, 1f);
        }
    }

    private static boolean hasRingPixels(Bitmap bitmap, int surfaceColor) {
        for (int y = 65; y < 71; y++) {
            for (int x = 205; x < 211; x++) {
                if (bitmap.getPixel(x, y) != surfaceColor) {
                    return true;
                }
            }
        }
        return false;
    }

    private static NavigationCompassView compactView(
            Activity activity, int size, float visibleRadius, float progress, Float accuracy
    ) {
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setCompassState(NavCompassState.fromProjectedPoints(
                0f, accuracy, 1f, visibleRadius,
                0f, true, 0f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                0f, 0f, false
        ));
        view.setPerspectiveProgress(progress);
        view.layout(0, 0, size, size);
        return view;
    }
}
