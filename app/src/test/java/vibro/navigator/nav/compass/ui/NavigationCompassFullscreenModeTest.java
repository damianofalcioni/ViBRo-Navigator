package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassFullscreenModeTest {
    @Test
    public void fullscreenRouteViewMeasuresToAvailableBounds() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setContentView(R.layout.activity_navigation);
        NavigationCompassView compact = activity.findViewById(R.id.navigationCompassView);
        NavigationCompassView fullscreen = activity.findViewById(R.id.navigationFullscreenCompassView);

        assertNotSame(compact.getParent(), fullscreen.getParent());
        compact.measure(exactly(300), exactly(500));
        fullscreen.measure(exactly(300), exactly(500));

        assertEquals(300, compact.getMeasuredWidth());
        assertEquals(300, compact.getMeasuredHeight());
        assertEquals(300, fullscreen.getMeasuredWidth());
        assertEquals(500, fullscreen.getMeasuredHeight());
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeFullscreenRouteViewStaysInCompassPanel() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setContentView(R.layout.activity_navigation);
        NavigationCompassView compact = activity.findViewById(R.id.navigationCompassView);
        NavigationCompassView fullscreen = activity.findViewById(R.id.navigationFullscreenCompassView);

        assertSame(compact.getParent(), fullscreen.getParent());
        compact.measure(exactly(300), exactly(500));
        fullscreen.measure(exactly(300), exactly(500));

        assertEquals(300, compact.getMeasuredWidth());
        assertEquals(300, compact.getMeasuredHeight());
        assertEquals(300, fullscreen.getMeasuredWidth());
        assertEquals(500, fullscreen.getMeasuredHeight());
    }

    @Test
    public void portraitFullscreenCompassDrawsOnlyFarthestDistanceTimePair() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        assertEquals(2, fullscreenCompassTextDrawCount(activity, 300, 500));
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeFullscreenCompassKeepsThreeDistanceTimePairs() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        assertEquals(6, fullscreenCompassTextDrawCount(activity, 500, 300));
    }

    @Test
    public void fullscreenGeometryKeepsCueBoundedAndRouteExpanded() {
        NavigationCompassFullscreenMode mode = new NavigationCompassFullscreenMode();

        assertEquals(412f, mode.resolveCenterY(500f, 88f), 0.01f);
        assertEquals(50f, mode.resolveCenterY(100f, 88f), 0.01f);
        assertEquals(478f, mode.resolveCenterY(500f, 88f, 478f), 0.01f);
        assertEquals(250f, mode.resolveCenterY(500f, 88f, 120f), 0.01f);
        assertEquals(412f, mode.resolveCenterY(500f, 88f, Float.NaN), 0.01f);
        assertEquals(140f, mode.resolveCompassRadius(150f, 412f, 10f), 0.01f);
        assertEquals(396f, mode.resolveRouteRadius(412f, 16f), 0.01f);
        assertEquals(2.83f, mode.resolveLegendOuterScale(396f, 140f), 0.01f);
        assertEquals(140f, mode.resolveHeadingGuideRadius(true, 396f, 140f), 0.01f);
        assertEquals(396f, mode.resolveHeadingGuideRadius(false, 396f, 140f), 0.01f);
        assertEquals(1f, mode.resolveLegendOuterScale(396f, 396f), 0.01f);
        assertEquals(1f, mode.resolveLegendOuterScale(396f, 0f), 0.01f);
    }

    private static int fullscreenCompassTextDrawCount(Activity activity, int width, int height) {
        NavigationCompassView compassView = new NavigationCompassView(activity);
        Canvas canvas = new Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
        compassView.setFullscreenRouteModeEnabled(true);
        compassView.setCompassState(compassState());
        compassView.layout(0, 0, width, height);

        compassView.draw(canvas);

        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        return shadowCanvas.getTextHistoryCount();
    }

    private static NavCompassState compassState() {
        return NavCompassState.fromProjectedPoints(
                0f,
                null,
                1f,
                80f,
                0f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                60f,
                0f,
                true
        );
    }

    private static int exactly(int value) {
        return View.MeasureSpec.makeMeasureSpec(value, View.MeasureSpec.EXACTLY);
    }
}
