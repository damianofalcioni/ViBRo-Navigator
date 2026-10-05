package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassPerspectiveLabelVisibilityTest {
    @Test
    public void sixtySecondLabelsRemainVisibleBeyondCompassOuterRing() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        compassView(activity, 30f).draw(new Canvas(bitmap));

        assertTrue("Visible outer distance label", labelPixels(bitmap, 265, 295, 52, 72) > 15);
        assertTrue("Visible outer 60s label", labelPixels(bitmap, 5, 35, 52, 72) > 15);
    }

    @Test
    public void wideAccuracyLabelsFitCompassBoundsOnLargerDrawingSurface() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Bitmap bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.clipRect(0, 0, 300, 300);
        compassView(activity, 85f).draw(canvas);

        assertTrue("Distance label inside compass view", labelPixels(bitmap, 280, 300, 156, 170) > 10);
        assertTrue("Time label inside compass view", labelPixels(bitmap, 0, 25, 156, 170) > 10);
    }

    private static NavigationCompassView compassView(Activity activity, float accuracy) {
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setCompassState(NavCompassState.fromProjectedPoints(
                0f, accuracy, 1f, 60f, 0f, true, 0f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                0f, 0f, false
        ));
        view.setPerspectiveProgress(1f);
        view.layout(0, 0, 300, 300);
        return view;
    }

    private static int labelPixels(Bitmap bitmap, int left, int right, int top, int bottom) {
        int count = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (Color.red(bitmap.getPixel(x, y)) > 80) {
                    count++;
                }
            }
        }
        return count;
    }
}
