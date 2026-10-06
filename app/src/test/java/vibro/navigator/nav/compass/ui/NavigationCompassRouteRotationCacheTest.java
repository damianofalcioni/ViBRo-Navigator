package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.orientation.NavigationCompassModeController;
import vibro.navigator.nav.orientation.NavigationCompassGestureState;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationCompassRouteRotationCacheTest {
    @Test
    public void perspectiveAndPinchPreparationKeepCachedPathsAcrossHeadingUpdates() {
        NavigationCompassModeController controller = new NavigationCompassModeController(() -> 0L);
        NavigationCompassGestureState gestures = new NavigationCompassGestureState();
        NavCompassState automatic = state(geometry(Collections.emptyList(), Collections.emptyList()), 0d, 0, false);
        controller.onCompassTapped(automatic, 0L, false);
        controller.onCompassTapped(automatic, 0L, false);
        NavCompassState perspective = controller.resolve(automatic, 320L, false);
        gestures.apply(perspective, true);
        gestures.zoomBy(1);
        NavCompassState displayed = gestures.apply(perspective, true);
        NavigationCompassFullResolutionRouteRenderer renderer = renderer();
        RecordingCanvas canvas = new RecordingCanvas();
        draw(renderer, canvas, displayed);
        Path first = canvas.paths.get(0);

        NavCompassState heading = NavCompassHeadingRefresh.apply(automatic, 90d, 12f);
        NavCompassState refreshed = gestures.apply(controller.resolve(heading, 400L, false), true);
        assertSame(displayed.routePoints, refreshed.routePoints);
        assertSame(displayed.fullRouteView, refreshed.fullRouteView);
        assertEquals(90f, refreshed.displayMode.headingDegrees, 0f);
        assertEquals(12f, refreshed.displayMode.headingAccuracyDegrees, 0f);
        assertEquals(displayed.radiusState.visibleRadiusMeters, refreshed.radiusState.visibleRadiusMeters, 0f);
        draw(renderer, canvas, refreshed);
        assertSame(first, canvas.paths.get(1));
    }

    @Test
    public void sampledAndFullResolutionPathsSurviveHeadingSnapshots() {
        assertHeadingReuse(false);
        assertHeadingReuse(true);
    }

    @Test
    public void locationZoomProgressAndRouteReplacementRebuildPaths() {
        NavigationCompassFullResolutionRouteRenderer renderer = renderer();
        RecordingCanvas canvas = new RecordingCanvas();
        CompassRouteGeometry geometry = geometry(Collections.emptyList(), Collections.emptyList());
        NavCompassState initial = state(geometry, 0d, 0, true);
        draw(renderer, canvas, initial);
        Path first = canvas.paths.get(canvas.paths.size() - 1);

        draw(renderer, canvas, state(geometry, 0.0001d, 0, true));
        Path moved = canvas.paths.get(canvas.paths.size() - 1);
        assertNotSame(first, moved);

        draw(renderer, canvas, initial.withDisplayMode(true, 400f));
        Path zoomed = canvas.paths.get(canvas.paths.size() - 1);
        assertNotSame(moved, zoomed);

        draw(renderer, canvas, state(geometry, 0d, 2, true));
        Path progressed = canvas.paths.get(canvas.paths.size() - 1);
        assertNotSame(zoomed, progressed);

        draw(renderer, canvas, state(geometry(Collections.emptyList(), Collections.emptyList()), 0d, 0, true));
        assertNotSame(progressed, canvas.paths.get(canvas.paths.size() - 1));
    }

    @Test
    public void archivedRouteAndBridgeHaveDistinctReusablePaths() {
        List<LatLon> archive = Arrays.asList(new LatLon(0d, -0.001d), new LatLon(0d, 0d));
        List<LatLon> bridge = Arrays.asList(new LatLon(0d, 0d), new LatLon(0d, 0.001d));
        NavCompassState state = state(geometry(
                Collections.singletonList(archive), Collections.singletonList(bridge)), 0d, 0, true);
        NavigationCompassFullResolutionRouteRenderer renderer = renderer();
        RecordingCanvas canvas = new RecordingCanvas();
        draw(renderer, canvas, state);
        int pathsPerFrame = canvas.paths.size();
        assertEquals(3, pathsPerFrame);
        assertNotSame(canvas.paths.get(0), canvas.paths.get(1));

        draw(renderer, canvas, NavCompassHeadingRefresh.apply(state, 90d, null));
        for (int i = 0; i < pathsPerFrame; i++) {
            assertSame(canvas.paths.get(i), canvas.paths.get(i + pathsPerFrame));
        }
    }

    private static void assertHeadingReuse(boolean moving) {
        NavigationCompassFullResolutionRouteRenderer renderer = renderer();
        RecordingCanvas canvas = new RecordingCanvas();
        NavCompassState initial = state(geometry(Collections.emptyList(), Collections.emptyList()), 0d, 0, moving);
        draw(renderer, canvas, initial);
        int pathsPerFrame = canvas.paths.size();
        for (double heading : new double[]{45d, 90d, 359d, 1d}) {
            draw(renderer, canvas, NavCompassHeadingRefresh.apply(initial, heading, 10f));
            for (int i = 0; i < pathsPerFrame; i++) {
                assertSame(canvas.paths.get(i), canvas.paths.get(canvas.paths.size() - pathsPerFrame + i));
            }
        }
        assertEquals(1, pathsPerFrame);
    }

    private static void draw(
            NavigationCompassFullResolutionRouteRenderer renderer, RecordingCanvas canvas, NavCompassState state
    ) {
        float scale = 100f / state.radiusState.visibleRadiusMeters;
        if (state.fullRouteView.isActive()) {
            renderer.drawRoute(canvas, state, 150f, 150f, scale, state.displayMode.headingDegrees, 24f, false);
        } else {
            renderer.drawSampledRoute(canvas, state, 150f, 150f, scale,
                    state.displayMode.headingDegrees, 24f, false);
        }
    }

    private static NavigationCompassFullResolutionRouteRenderer renderer() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        return new NavigationCompassFullResolutionRouteRenderer(paint, paint, paint, paint, paint);
    }

    private static NavCompassState state(CompassRouteGeometry geometry, double latitude, int passed, boolean moving) {
        return NavCompassState.fromRouteGeometry(0f, null, 1f, 1f, 1f,
                200f, 200f, 200f, 5f, moving, 0f, geometry,
                latitude, 0d, passed, 0f, 100f, 5f, true);
    }

    private static CompassRouteGeometry geometry(List<List<LatLon>> archives, List<List<LatLon>> bridges) {
        return new CompassRouteGeometry(Arrays.asList(
                new CompassRouteGeometry.SamplePoint(new LatLon(-0.001d, 0d), 0d),
                new CompassRouteGeometry.SamplePoint(new LatLon(0d, 0d), 111d),
                new CompassRouteGeometry.SamplePoint(new LatLon(0.001d, 0d), 222d)),
                Collections.emptyList(), Collections.emptyList(), archives, bridges);
    }

    private static final class RecordingCanvas extends Canvas {
        final List<Path> paths = new ArrayList<>();

        RecordingCanvas() {
            super(Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888));
        }

        @Override
        public void drawPath(Path path, Paint paint) {
            paths.add(path);
            super.drawPath(path, paint);
        }
    }
}
