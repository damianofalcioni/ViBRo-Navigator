package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.settings.AppCompassSettings;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassCentralViewTest {
    @Test
    public void centralProjectionKeepsRoutesAndStreetsVisibleAtTheForwardEdgeInPortrait() {
        assertForwardGeometry(false, 300, 300);
        assertForwardGeometry(true, 300, 500);
    }

    @Test
    @Config(qualifiers = "land")
    public void centralProjectionKeepsRoutesAndStreetsVisibleAtTheForwardEdgeInLandscape() {
        assertForwardGeometry(true, 500, 300);
    }

    @Test
    public void preferenceLeaves2dPixelsUnchangedAndCanRestoreOriginalTilt() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView view = new NavigationCompassView(activity);
        view.layout(0, 0, 300, 300);
        view.setCompassState(state(true, true).withDisplayMode(true, 100f));
        AppCompassSettings.setCentralPerspectiveEnabled(activity, false);
        Bitmap flat = draw(view, 300, 300);
        AppCompassSettings.setCentralPerspectiveEnabled(activity, true);
        assertTrue(flat.sameAs(draw(view, 300, 300)));
        view.setPerspectiveProgress(1f);
        view.setCompassState(state(true, true).withDisplayMode(true, 600f));
        Bitmap central = draw(view, 300, 300);
        AppCompassSettings.setCentralPerspectiveEnabled(activity, false);
        view.setCompassState(state(true, true).withDisplayMode(true,
                100f * CompassPerspectiveScale.maximumViewportMultiplier()));
        Bitmap original = draw(view, 300, 300);
        assertTrue("Perspective must change the rendered geometry", !central.sameAs(original));
        AppCompassSettings.setCentralPerspectiveEnabled(activity, true);
        view.setCompassState(state(true, true).withDisplayMode(true, 600f));
        assertTrue(central.sameAs(draw(view, 300, 300)));
    }

    private static void assertForwardGeometry(boolean fullscreen, int width, int height) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AppCompassSettings.setCentralPerspectiveEnabled(activity, true);
        AppCompassSettings.setDistanceCirclesEnabled(activity, false);
        NavigationCompassView view = new NavigationCompassView(activity);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.layout(0, 0, width, height);
        for (float inclination : new float[] {0.25f, 1f, 1.25f}) {
            view.setPerspectiveProgress(inclination);
            view.setCompassState(state(false, false));
            Bitmap empty = draw(view, width, height);
            view.setCompassState(state(true, false));
            assertTrue("Route must reach the upper part of the viewport",
                    forwardDifferences(empty, draw(view, width, height)) > 10);
            view.setCompassState(state(false, true));
            assertTrue("Streets must reach the upper part of the viewport",
                    forwardDifferences(empty, draw(view, width, height)) > 10);
        }
    }

    private static int forwardDifferences(Bitmap first, Bitmap second) {
        int count = 0;
        for (int y = 30; y < 100; y++) {
            for (int x = 20; x < first.getWidth() - 20; x++) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Bitmap draw(NavigationCompassView view, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static NavCompassState state(boolean route, boolean streets) {
        CompassRouteGeometry geometry = new CompassRouteGeometry(route ? Arrays.asList(
                new CompassRouteGeometry.SamplePoint(new LatLon(-0.002, 0.00015), 0.0),
                new CompassRouteGeometry.SamplePoint(new LatLon(0.01, 0.00015), 1_300.0)
        ) : Collections.emptyList(), Collections.emptyList());
        NavCompassState state = NavCompassState.fromRouteGeometry(
                0f, null, 1f, 1f, 1f, 600f, 1_000f, 100f,
                0f, true, 0f, geometry, 0.0, 0.0, 0,
                0f, 0f, 0f, false);
        CompassStreetSegment street = new CompassStreetSegment(Arrays.asList(
                new LatLon(-0.002, -0.00015), new LatLon(0.01, -0.00015)));
        return streets ? state.withStreetOverlay(new CompassStreetOverlay(Collections.singletonList(street))) : state;
    }
}
