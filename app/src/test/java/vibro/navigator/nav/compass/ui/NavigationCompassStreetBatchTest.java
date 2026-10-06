package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassStreetBatchTest {
    @Test
    public void compactAndFullscreen2dReuseStreetBatches() {
        assertViewBatches(300, 500, false, 0f);
        assertViewBatches(300, 500, true, 0f);
    }

    @Test
    public void compactAndFullscreenPerspectiveReuseStreetBatches() {
        assertViewBatches(300, 500, false, 1f);
        assertViewBatches(300, 500, true, 1f);
    }

    @Test
    @Config(qualifiers = "land")
    public void landscape2dAndPerspectiveReuseStreetBatches() {
        assertViewBatches(500, 300, false, 0f);
        assertViewBatches(500, 300, true, 0f);
        assertViewBatches(500, 300, false, 1f);
        assertViewBatches(500, 300, true, 1f);
    }

    private static void assertViewBatches(int width, int height, boolean fullscreen, float perspective) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassView view = new NavigationCompassView(activity);
        NavCompassState state = state();
        RecordingCanvas canvas = new RecordingCanvas();
        view.layout(0, 0, width, height);
        view.setCompassState(state);
        view.setFullscreenRouteModeEnabled(fullscreen);
        view.setPerspectiveProgress(perspective);
        view.draw(canvas);
        float[] initial = canvas.lastLines;
        assertEquals(1, canvas.batches);
        assertEquals(8, canvas.lastCount);

        view.setCompassState(NavCompassHeadingRefresh.apply(state, 90d, null));
        view.draw(canvas);
        assertEquals(2, canvas.batches);
        assertSame(initial, canvas.lastLines);

        if (perspective > 0f) {
            view.setPerspectiveProgress(1.25f);
            view.draw(canvas);
            assertEquals(3, canvas.batches);
            assertSame(initial, canvas.lastLines);
        }

        view.setCompassState(state.withDisplayMode(true, 200f));
        view.draw(canvas);
        assertNotSame(initial, canvas.lastLines);
        view.setFullscreenRouteModeEnabled(!fullscreen);
        int beforeSwitch = canvas.batches;
        view.draw(canvas);
        assertEquals(beforeSwitch + 1, canvas.batches);
    }

    @Test
    public void batchesKeepStreetEndpointsAndDoNotConnectSeparateStreets() {
        NavigationStreetGeometry geometry = new NavigationStreetGeometry();
        geometry.moveTo(10f, 20f);
        geometry.lineTo(20f, 30f);
        geometry.lineTo(20.1f, 30.1f);
        geometry.moveTo(100f, 120f);
        geometry.lineTo(110f, 130f);
        RecordingCanvas canvas = new RecordingCanvas();
        geometry.draw(canvas, new Paint());

        assertEquals(12, canvas.lastCount);
        assertEquals(20f, canvas.lastLines[4], 0f);
        assertEquals(20.1f, canvas.lastLines[6], 0f);
        assertEquals(100f, canvas.lastLines[8], 0f);
        assertEquals(110f, canvas.lastLines[10], 0f);
    }

    @Test
    public void roundCappedSegmentsStayContinuousWithoutJoiningSeparateStreets() {
        NavigationStreetGeometry geometry = new NavigationStreetGeometry();
        geometry.moveTo(10f, 50f);
        geometry.lineTo(60f, 50f);
        geometry.lineTo(60f, 100f);
        geometry.moveTo(160f, 50f);
        geometry.lineTo(200f, 50f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Bitmap bitmap = Bitmap.createBitmap(300, 150, Bitmap.Config.ARGB_8888);
        geometry.draw(new Canvas(bitmap), paint);

        assertTrue(bitmap.getPixel(8, 50) != 0);
        assertTrue(bitmap.getPixel(60, 50) != 0);
        assertTrue(bitmap.getPixel(60, 99) != 0);
        assertEquals(0, bitmap.getPixel(100, 50));
    }

    @Test
    public void denseStreetBatchesReuseStorageAcrossHeadingChangesAndRebuildForNewOverlay() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();
        NavCompassState state = state();
        CompassStreetOverlay overlay = new CompassStreetOverlay(
                Collections.nCopies(1000, state.streetOverlay.segments.get(0)));
        state = state.withStreetOverlay(overlay);
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.draw(canvas, activity, state, 150f, 350f, 400f, 0f);
        float[] initial = canvas.lastLines;
        assertEquals(8000, canvas.lastCount);
        for (float heading : new float[]{45f, 90f, 359f, 1f}) {
            renderer.draw(canvas, activity, state, 150f, 350f, 400f, heading);
            assertSame(initial, canvas.lastLines);
        }
        renderer.draw(canvas, activity, state(), 150f, 350f, 400f, 0f);
        assertNotSame(initial, canvas.lastLines);
    }

    private static NavCompassState state() {
        CompassRouteGeometry geometry = new CompassRouteGeometry(Arrays.asList(
                new CompassRouteGeometry.SamplePoint(new LatLon(0d, 0d), 0d),
                new CompassRouteGeometry.SamplePoint(new LatLon(0d, 0.001d), 100d)
        ), Collections.emptyList());
        NavCompassState state = NavCompassState.fromRouteGeometry(
                0f, null, 1f, 1f, 1f, 100f, 1000f, 100f, 5f, true, 0f,
                geometry, 0d, 0d, 0, 100f, 0f, 5f, true);
        CompassStreetSegment street = new CompassStreetSegment(Arrays.asList(
                new LatLon(-0.001d, -0.00015d),
                new LatLon(0d, -0.00015d),
                new LatLon(0.001d, -0.00015d)));
        return state.withStreetOverlay(new CompassStreetOverlay(Collections.singletonList(street)));
    }

    private static final class RecordingCanvas extends Canvas {
        float[] lastLines;
        int lastCount;
        int batches;

        RecordingCanvas() {
            super(Bitmap.createBitmap(300, 500, Bitmap.Config.ARGB_8888));
        }

        @Override
        public void drawLines(float[] points, int offset, int count, Paint paint) {
            assertEquals(0, offset);
            assertTrue(count > 0);
            lastLines = points;
            lastCount = count;
            batches++;
            super.drawLines(points, offset, count, paint);
        }
    }
}
