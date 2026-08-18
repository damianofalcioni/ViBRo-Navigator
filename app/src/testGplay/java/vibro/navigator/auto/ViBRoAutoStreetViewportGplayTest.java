package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassState;
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
    }

    @Test
    public void compassPainterPublishesDrawnStreetViewport() throws Exception {
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        RecordingAutoControls controls = new RecordingAutoControls();
        RecordingStreetViewportSink streetViewportSink = new RecordingStreetViewportSink();
        ViBRoAutoCompassPainter painter = new ViBRoAutoCompassPainter(
                carContext,
                controls,
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
        RecordingAutoControls controls = new RecordingAutoControls();
        RecordingStreetViewportSink streetViewportSink = new RecordingStreetViewportSink();
        ViBRoAutoSurfaceRenderer renderer = new ViBRoAutoSurfaceRenderer(
                testCarContext(),
                controls,
                Runnable::run,
                streetViewportSink
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
                        new NavProgressStatus("ETA 13:51", "466 m", ""),
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

    private static final class RecordingAutoControls implements ViBRoAutoSurfaceRenderer.Controls {
        @Override
        public void onBlockedRoad() {
        }

        @Override
        public void onStopNavigation() {
        }

        @Override
        public void onTogglePaused() {
        }

        @Override
        public void onToggleCustomButton() {
        }

        @Override
        @NonNull
        public String buildCurrentDirectionDetailsText() {
            return "";
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
