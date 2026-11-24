package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

@RunWith(RobolectricTestRunner.class)
public class ViBRoAutoStreetViewportGplayTest {
    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void compassPainterPublishesDrawnStreetViewport() throws Exception {
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        RecordingStreetViewportSink streetViewportSink = new RecordingStreetViewportSink();
        ViBRoAutoCompassPainter painter = new ViBRoAutoCompassPainter(
                carContext,
                streetViewportSink,
                () -> 1_000L
        );
        NavCompassState compassState = movingCompassState();
        Bitmap bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);

        painter.draw(
                new Canvas(bitmap),
                activeNavigationState(compassState),
                0f,
                0f,
                240f,
                240f,
                false,
                new RectF(0f, 0f, 240f, 240f),
                1f
        );

        assertSame(compassState, streetViewportSink.lastCompassStreetViewport);
        assertEquals(1, streetViewportSink.compassStreetViewportUpdates);
    }

    @Test
    public void surfaceRendererClearsStreetViewportWhenInactiveOrDestroyed() throws Exception {
        RecordingStreetViewportSink streetViewportSink = new RecordingStreetViewportSink();
        ViBRoAutoSurfaceRenderer renderer = new ViBRoAutoSurfaceRenderer(
                testCarContext(),
                Runnable::run,
                streetViewportSink,
                () -> { }
        );

        streetViewportSink.lastCompassStreetViewport = movingCompassState();
        renderer.setState(null);

        assertNull(streetViewportSink.lastCompassStreetViewport);
        assertEquals(1, streetViewportSink.compassStreetViewportUpdates);

        streetViewportSink.lastCompassStreetViewport = movingCompassState();
        renderer.clearSurface();

        assertNull(streetViewportSink.lastCompassStreetViewport);
        assertEquals(2, streetViewportSink.compassStreetViewportUpdates);
    }

    @Test
    public void hostScaleUpdatesPublishedViewportAndResetsWithNavigation() throws Exception {
        PainterFixture fixture = painterFixture();
        assertTrue(fixture.painter.handleScale(120f, 120f, 2f, fixture.state));
        fixture.draw();
        assertEquals(150f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
        assertFalse(fixture.painter.handleScale(120f, 120f, 2f, fixture.state));
        fixture.painter.reset();
        fixture.draw();
        assertEquals(300f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0f);
        assertTrue(fixture.painter.handleScale(-1f, -1f, 0.5f, fixture.state));
        fixture.draw();
        assertEquals(600f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0f);
    }

    @Test
    public void hostScaleRejectsOutsideFocusAndOverview() throws Exception {
        PainterFixture fixture = painterFixture();
        assertFalse(fixture.painter.handleScale(400f, 120f, 2f, fixture.state));
        fixture.painter.handleScroll(-100f, 0f, fixture.state);
        fixture.draw();
        assertFalse(fixture.viewport.lastCompassStreetViewport.displayMode.movingScaleActive);
        fixture.timeMs += 400L;
        assertFalse(fixture.painter.handleScale(120f, 120f, 2f, fixture.state));
    }

    @Test
    public void horizontalScrollCyclesForwardAndBackwardWithTheShared3dViewport() throws Exception {
        PainterFixture fixture = painterFixture();
        assertTrue(fixture.painter.handleScroll(100f, 0f, fixture.state));
        fixture.timeMs += 400L;
        fixture.draw();
        float perspectiveRadius = 300f * CompassPerspectiveScale.maximumViewportMultiplier(true);
        assertEquals(perspectiveRadius, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
        assertTrue(fixture.painter.handleScroll(-100f, 0f, fixture.state));
        fixture.timeMs += 400L;
        fixture.draw();
        assertEquals(300f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
    }

    @Test
    public void verticalScrollTiltsOnly3dAndReusesThePreparedViewport() throws Exception {
        PainterFixture fixture = painterFixture();
        assertFalse(fixture.painter.handleScroll(0f, -100f, fixture.state));
        fixture.timeMs += 400L;
        fixture.painter.handleClick(120f, 120f, fixture.state);
        fixture.timeMs += 400L;
        fixture.draw();
        NavCompassState perspective = fixture.viewport.lastCompassStreetViewport;
        assertTrue(fixture.painter.handleScroll(0f, -100f, fixture.state));
        fixture.draw();
        assertSame(perspective, fixture.viewport.lastCompassStreetViewport);
        assertEquals(0.375f, fixture.perspectiveProgress(), 0.001f);
        fixture.timeMs += 400L;
        assertTrue(fixture.painter.handleScroll(0f, 200f, fixture.state));
        fixture.draw();
        assertEquals(1.25f, fixture.perspectiveProgress(), 0.001f);
    }

    @Test
    public void perspectivePreferenceUpdatesThePublishedViewportAndRetainsHostTilt() throws Exception {
        AppCompassSettings.setCentralPerspectiveEnabled(context, false);
        PainterFixture fixture = painterFixture();
        fixture.painter.handleScroll(100f, 0f, fixture.state);
        fixture.timeMs += 400L;
        fixture.draw();
        assertEquals(1f, fixture.perspectiveProgress(), 0.001f);
        AppCompassSettings.setCentralPerspectiveEnabled(context, true);
        fixture.draw();
        NavCompassState central = fixture.viewport.lastCompassStreetViewport;
        assertEquals(300f * CompassPerspectiveScale.maximumViewportMultiplier(true),
                central.radiusState.visibleRadiusMeters, 0.01f);
        fixture.painter.handleScroll(0f, -100f, fixture.state);
        fixture.draw();
        assertSame(central, fixture.viewport.lastCompassStreetViewport);
        assertEquals(0.375f, fixture.perspectiveProgress(), 0.001f);
        AppCompassSettings.setCentralPerspectiveEnabled(context, false);
        fixture.draw();
        assertEquals(300f * CompassPerspectiveScale.maximumViewportMultiplier(),
                fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(0.375f, fixture.perspectiveProgress(), 0.001f);
    }

    @Test
    public void releaseClickCannotCycleAfterScalingAndSwipeIsConsumed() throws Exception {
        PainterFixture fixture = painterFixture();
        fixture.painter.handleScale(120f, 120f, 2f, fixture.state);
        fixture.painter.handleClick(120f, 120f, fixture.state);
        fixture.draw();
        assertEquals(150f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
        assertEquals(0f, fixture.perspectiveProgress(), 0f);
        fixture.timeMs += 400L;
        assertTrue(fixture.painter.handleScroll(100f, 0f, fixture.state));
        assertFalse(fixture.painter.handleScroll(100f, 0f, fixture.state));
        fixture.timeMs += 400L;
        fixture.draw();
        assertEquals(1f, fixture.perspectiveProgress(), 0f);
    }

    @Test
    public void instantZoomSettingAppliesToAutoViewSwitches() throws Exception {
        PainterFixture fixture = painterFixture();
        AppCompassSettings.setInstantZoomEnabled(context, true);
        fixture.painter.handleScroll(-100f, 0f, fixture.state);
        fixture.draw();
        assertFalse(fixture.painter.isTransitionInProgress());
        assertFalse(fixture.viewport.lastCompassStreetViewport.displayMode.movingScaleActive);
    }

    @Test
    public void formerCustomButtonAreaNowUsesMapGestures() throws Exception {
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        PainterFixture fixture = painterFixture();
        assertTrue(fixture.painter.handleScale(220f, 20f, 2f, fixture.state));
        fixture.draw();
        assertEquals(150f, fixture.viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0f);
    }

    @Test
    public void destroyedOrInactiveSurfaceIgnoresHostGestures() throws Exception {
        RecordingStreetViewportSink viewport = new RecordingStreetViewportSink();
        ViBRoAutoSurfaceRenderer renderer = new ViBRoAutoSurfaceRenderer(testCarContext(),
                Runnable::run, viewport, () -> { });
        renderer.setState(activeNavigationState(movingCompassState()));
        renderer.onScroll(100f, 0f);
        renderer.onScale(-1f, -1f, 2f);
        assertEquals(0, viewport.compassStreetViewportUpdates);
        renderer.setState(null);
        renderer.onScroll(100f, 0f);
        renderer.onScale(-1f, -1f, 2f);
        assertEquals(1, viewport.compassStreetViewportUpdates);
    }

    @Test
    public void changingMapThemeRetainsManualZoom() throws Exception {
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator_Light);
        RecordingStreetViewportSink viewport = new RecordingStreetViewportSink();
        ViBRoAutoSurfacePainter painter = new ViBRoAutoSurfacePainter(carContext, viewport, () -> 1_000L);
        NavState state = activeNavigationState(movingCompassState());
        Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));
        SurfaceContainer surface = new SurfaceContainer(null, 240, 240, 160);
        Rect area = new Rect(0, 0, 240, 240);
        painter.draw(canvas, surface, area, state);
        assertTrue(painter.handleScale(120, 120, 2, state));

        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        painter.refreshTheme();
        painter.draw(canvas, surface, area, state);

        assertEquals(150f, viewport.lastCompassStreetViewport.radiusState.visibleRadiusMeters, 0.01f);
    }

    private PainterFixture painterFixture() throws Exception {
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        PainterFixture fixture = new PainterFixture();
        fixture.painter = new ViBRoAutoCompassPainter(carContext,
                fixture.viewport, () -> fixture.timeMs);
        fixture.draw();
        return fixture;
    }

    private static final class PainterFixture {
        private final RecordingStreetViewportSink viewport = new RecordingStreetViewportSink();
        private final NavState state = activeNavigationState(movingCompassState());
        private final Canvas canvas = new Canvas(Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888));
        private long timeMs = 1_000L;
        private ViBRoAutoCompassPainter painter;

        void draw() {
            painter.draw(canvas, state, 0f, 0f, 240f, 240f, false, new RectF(0f, 0f, 240f, 240f), 1f);
        }

        float perspectiveProgress() throws Exception {
            Field view = ViBRoAutoCompassPainter.class.getDeclaredField("compassView");
            view.setAccessible(true);
            Object compassView = view.get(painter);
            Field progress = compassView.getClass().getDeclaredField("perspectiveProgress");
            progress.setAccessible(true);
            return progress.getFloat(compassView);
        }
    }

    @NonNull
    private CarContext testCarContext() throws Exception {
        CarContext carContext = CarContext.create(new TestLifecycleOwner().getLifecycle());
        Method attachBaseContext = CarContext.class.getDeclaredMethod(
                "attachBaseContext",
                Context.class,
                Configuration.class
        );
        attachBaseContext.setAccessible(true);
        attachBaseContext.invoke(carContext, context, context.getResources().getConfiguration());
        return carContext;
    }

    @NonNull
    private static NavState activeNavigationState(@Nullable NavCompassState compassState) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus("Turn right", "Continue"),
                        new NavProgressStatus("13:51", "466 m", ""),
                        compassState
                ),
                new NavGpsStatus("0 km/h", NavState.NO_DEADLINE),
                new NavPauseStatus(false)
        );
    }

    @NonNull
    private static NavCompassState movingCompassState() {
        return NavCompassState.fromProjectedPoints(
                90f,
                8f,
                5f,
                300f,
                5f,
                true,
                13f,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0f,
                1_000f,
                false
        );
    }

    private static final class TestLifecycleOwner implements LifecycleOwner {
        private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

        @Override
        @NonNull
        public Lifecycle getLifecycle() {
            return lifecycle;
        }
    }

    private static final class RecordingStreetViewportSink implements ViBRoAutoCompassStreetViewportSink {
        @Nullable
        private NavCompassState lastCompassStreetViewport;
        private int compassStreetViewportUpdates;

        @Override
        public void onCompassStreetViewport(@Nullable NavCompassState compassState) {
            lastCompassStreetViewport = compassState;
            compassStreetViewportUpdates++;
        }
    }
}
