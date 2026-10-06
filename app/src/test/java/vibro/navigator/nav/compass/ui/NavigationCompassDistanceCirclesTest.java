package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCanvas;

import java.util.Collections;

import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassDistanceCirclesTest {
    @Test
    public void settingHidesAndRestoresOverlayInPortraitModes() {
        assertModes(300, 500);
    }

    @Test
    @Config(qualifiers = "land")
    public void settingHidesAndRestoresOverlayInLandscapeModes() {
        assertModes(500, 300);
    }

    private static void assertModes(int width, int height) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (boolean fullscreen : new boolean[]{false, true}) {
            for (float progress : new float[]{0f, 0.5f, 1f}) {
                assertOverlayToggle(activity, width, height, fullscreen, progress);
            }
        }
    }

    private static void assertOverlayToggle(
            Activity activity, int width, int height, boolean fullscreen, float progress
    ) {
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.setPerspectiveProgress(progress);
        view.setCompassState(NavCompassState.fromProjectedPoints(
                0f, 20f, 1f, 100f, 0f,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0f, 0f, false));
        view.layout(0, 0, width, height);
        AppCompassSettings.setDistanceCirclesEnabled(activity, true);
        ShadowCanvas enabled = draw(view, width, height);
        AppCompassSettings.setDistanceCirclesEnabled(activity, false);
        ShadowCanvas disabled = draw(view, width, height);

        assertEquals(fullscreen ? 1 : 2,
                enabled.getCirclePaintHistoryCount() - disabled.getCirclePaintHistoryCount());
        assertEquals(5, enabled.getLinePaintHistoryCount() - disabled.getLinePaintHistoryCount());
        assertEquals(0, disabled.getArcPaintHistoryCount());
        assertTrue(enabled.getArcPaintHistoryCount() > 0);
        assertEquals(fullscreen ? 0 : 4, disabled.getTextHistoryCount());
        assertTrue(enabled.getTextHistoryCount() > disabled.getTextHistoryCount());
        assertTrue("Position marker stays visible", disabled.getCirclePaintHistoryCount() > 0);

        AppCompassSettings.setDistanceCirclesEnabled(activity, true);
        ShadowCanvas restored = draw(view, width, height);
        assertEquals(enabled.getCirclePaintHistoryCount(), restored.getCirclePaintHistoryCount());
        assertEquals(enabled.getLinePaintHistoryCount(), restored.getLinePaintHistoryCount());
        assertEquals(enabled.getArcPaintHistoryCount(), restored.getArcPaintHistoryCount());
        assertEquals(enabled.getTextHistoryCount(), restored.getTextHistoryCount());
    }

    private static ShadowCanvas draw(NavigationCompassView view, int width, int height) {
        Canvas canvas = new Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
        view.draw(canvas);
        return Shadows.shadowOf(canvas);
    }
}
