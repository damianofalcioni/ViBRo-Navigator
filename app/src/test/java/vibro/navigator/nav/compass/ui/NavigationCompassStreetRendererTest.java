package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.NavCompassState;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassStreetRendererTest {
    @Test
    public void surroundingStreetOverlayUsesCategoryColorsInDarkTheme() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_ViBRoNavigator);
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();

        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.HIGHWAY),
                platformThemeColor(activity, R.attr.vibroCompassStreetHighwayColor),
                204
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.NORMAL),
                platformThemeColor(activity, R.attr.vibroCompassStreetNormalColor),
                204
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.WALKING_CYCLING),
                platformThemeColor(activity, R.attr.vibroCompassStreetWalkingCyclingColor),
                204
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.SPECIAL_ROUTING),
                platformThemeColor(activity, R.attr.vibroCompassStreetSpecialRoutingColor),
                204
        );
    }

    @Test
    public void surroundingStreetOverlayUsesStrongerCategoryColorsInLightTheme() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_ViBRoNavigator_Light);
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();

        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.HIGHWAY),
                platformThemeColor(activity, R.attr.vibroCompassStreetHighwayColor),
                255
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.NORMAL),
                platformThemeColor(activity, R.attr.vibroCompassStreetNormalColor),
                255
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.WALKING_CYCLING),
                platformThemeColor(activity, R.attr.vibroCompassStreetWalkingCyclingColor),
                255
        );
        assertPaint(
                renderer.paintForTest(activity, CompassStreetCategory.SPECIAL_ROUTING),
                platformThemeColor(activity, R.attr.vibroCompassStreetSpecialRoutingColor),
                255
        );
    }

    @Test
    public void surroundingStreetOverlayDrawsOnlyInZoomedMovingScaleView() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();

        assertEquals(0, pathDrawCountAfterDraw(activity, renderer, compassState(false)));
        assertEquals(1, pathDrawCountAfterDraw(activity, renderer, compassState(true)));
    }

    @Test
    public void headingChangesRotateOneCachedPathForAllStreets() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();
        NavCompassState state = compassState(true).withStreetOverlay(new CompassStreetOverlay(
                Collections.nCopies(1000, streetOverlay().segments.get(0))
        ));
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.draw(canvas, activity, state, 100f, 100f, 80f, 0f);
        Path first = canvas.paths.get(0);
        assertEquals(1, canvas.draws);

        renderer.draw(canvas, activity, state, 100f, 100f, 80f, 90f);

        assertEquals(2, canvas.draws);
        assertEquals(-90f, canvas.heading, 0f);
        assertSame(first, canvas.paths.get(1));
        assertEquals(
                1.2f * activity.getResources().getDisplayMetrics().density,
                renderer.paintForTest(activity, CompassStreetCategory.SPECIAL_ROUTING).getStrokeWidth(),
                0.001f
        );
    }

    @Test
    public void drawsEachVisibleStreetCategoryOnceInBackgroundToForegroundOrder() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_ViBRoNavigator);
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();
        NavCompassState state = compassState(true).withStreetOverlay(new CompassStreetOverlay(Arrays.asList(
                streetSegment(CompassStreetType.MOTORWAY, 0d),
                streetSegment(CompassStreetType.RESIDENTIAL, 0.0001d),
                streetSegment(CompassStreetType.FOOTWAY, 0.0002d),
                streetSegment(CompassStreetType.RAILWAY, 0.0003d)
        )));
        RecordingCanvas canvas = new RecordingCanvas();

        renderer.draw(canvas, activity, state, 100f, 100f, 80f, 0f);

        assertEquals(4, canvas.draws);
        assertEquals(platformThemeColor(activity, R.attr.vibroCompassStreetSpecialRoutingColor),
                canvas.colors.get(0).intValue());
        assertEquals(platformThemeColor(activity, R.attr.vibroCompassStreetWalkingCyclingColor),
                canvas.colors.get(1).intValue());
        assertEquals(platformThemeColor(activity, R.attr.vibroCompassStreetNormalColor),
                canvas.colors.get(2).intValue());
        assertEquals(platformThemeColor(activity, R.attr.vibroCompassStreetHighwayColor),
                canvas.colors.get(3).intValue());
    }

    private static final class RecordingCanvas extends Canvas {
        final List<Path> paths = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        int draws;
        float heading;

        RecordingCanvas() {
            super(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888));
        }

        @Override
        public void rotate(float degrees) {
            heading = degrees;
            super.rotate(degrees);
        }

        @Override
        public void drawPath(Path path, Paint paint) {
            paths.add(path);
            colors.add(paint.getColor());
            draws++;
            super.drawPath(path, paint);
        }
    }

    private static int pathDrawCountAfterDraw(
            Activity activity,
            NavigationCompassStreetRenderer renderer,
            NavCompassState state
    ) {
        Canvas canvas = new Canvas(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888));
        renderer.draw(canvas, activity, state, 100f, 100f, 80f, 0f);
        ShadowCanvas shadowCanvas = Shadows.shadowOf(canvas);
        return shadowCanvas.getPathPaintHistoryCount();
    }

    private static NavCompassState compassState(boolean movingScaleActive) {
        CompassRouteGeometry routeGeometry = new CompassRouteGeometry(
                Arrays.asList(
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.0), 0.0),
                        new CompassRouteGeometry.SamplePoint(new LatLon(0.0, 0.001), 100.0)
                ),
                Collections.emptyList()
        );
        NavCompassState state = NavCompassState.fromRouteGeometry(
                0f,
                null,
                1f,
                1f,
                1f,
                100f,
                1_000f,
                100f,
                5f,
                movingScaleActive,
                0f,
                routeGeometry,
                0.0,
                0.0,
                0,
                0f,
                100f,
                5f,
                true
        );
        return state.withStreetOverlay(streetOverlay());
    }

    private static CompassStreetOverlay streetOverlay() {
        CompassStreetSegment streetSegment = new CompassStreetSegment(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0005, 0.0)
        ));
        return new CompassStreetOverlay(Collections.singletonList(streetSegment));
    }

    private static CompassStreetSegment streetSegment(CompassStreetType type, double longitude) {
        return new CompassStreetSegment(Arrays.asList(
                new LatLon(0.0, longitude),
                new LatLon(0.0005, longitude)
        ), type);
    }

    private static void assertPaint(Paint paint, int color, int alpha) {
        assertEquals(color, paint.getColor());
        assertEquals(alpha, paint.getAlpha());
        assertEquals(Paint.Style.STROKE, paint.getStyle());
        assertNull(paint.getPathEffect());
    }

    private static int platformThemeColor(Activity activity, int attrResId) {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(attrResId, value, true);
        if (value.resourceId != 0) {
            return ContextCompat.getColor(activity, value.resourceId);
        }
        return value.data;
    }
}
