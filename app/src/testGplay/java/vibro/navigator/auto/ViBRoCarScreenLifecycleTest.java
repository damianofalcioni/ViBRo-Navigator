package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.util.ReflectionHelpers.getField;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.Surface;

import androidx.car.app.CarContext;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.ArrayDeque;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.service.NavigationService;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarScreenLifecycleTest {
    @Test
    public void idleAttachmentPollingStopsWhileCoveredAndRestartsWhenShown() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        CarContext context = contexts.testCarContext();
        Application application = ApplicationProvider.getApplicationContext();
        Shadows.shadowOf(application).declareComponentUnbindable(new ComponentName(context, NavigationService.class));
        RecordingScheduler scheduler = new RecordingScheduler();
        ViBRoCarScreen screen = new ViBRoCarScreen(context, scheduler);
        LifecycleRegistry lifecycle = (LifecycleRegistry) screen.getLifecycle();

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        scheduler.pending.remove().run();
        assertEquals(1, scheduler.pending.size());
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        assertEquals(0, scheduler.pending.size());
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertEquals(1, scheduler.pending.size());
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        assertEquals(0, scheduler.pending.size());
    }

    @Test
    public void mapVisibilityTracksTheNavigationTemplateAndSurface() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        ViBRoCarScreen screen = new ViBRoCarScreen(contexts.testCarContext(), ignored -> { });
        LifecycleRegistry lifecycle = (LifecycleRegistry) screen.getLifecycle();
        ViBRoCarNavigationController controller = getField(screen, "navigationController");
        ViBRoCarNavigationController.Host host = getField(controller, "host");
        ViBRoAutoSurfaceRenderer renderer = getField(screen, "surfaceRenderer");
        SurfaceTexture texture = new SurfaceTexture(0);
        Surface surface = new Surface(texture);
        try {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
            host.updateCurrentState(CarNavigationTestStates.active(false, false));
            assertTrue(screen.onGetTemplate() instanceof NavigationTemplate);
            assertFalse(mapVisible(controller));
            SurfaceContainer container = new SurfaceContainer(surface, 800, 480, 160);
            renderer.onSurfaceAvailable(container);
            assertTrue(mapVisible(controller));

            screen.handleIntent(new Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:48.2,16.3")));
            PaneTemplate pane = (PaneTemplate) screen.onGetTemplate();
            assertFalse(mapVisible(controller));
            assertTrue(ViBRoCarHostNavigation.isNavigating(host.currentState()));
            renderer.onSurfaceDestroyed(container);
            pane.getPane().getActions().get(0).getOnClickDelegate().sendClick(new OnDoneCallback() { });
            assertTrue(screen.onGetTemplate() instanceof NavigationTemplate);
            assertFalse(mapVisible(controller));
            renderer.onSurfaceAvailable(container);
            assertTrue(mapVisible(controller));

            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            renderer.onSurfaceAvailable(container);
            assertFalse(mapVisible(controller));
        } finally {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
            surface.release();
            texture.release();
        }
    }

    private static boolean mapVisible(ViBRoCarNavigationController controller) {
        return getField(controller, "mapVisible");
    }

    private static final class RecordingScheduler implements TaskScheduler {
        final ArrayDeque<Runnable> pending = new ArrayDeque<>();

        @Override public void post(Runnable action) { pending.add(action); }
        @Override public void removeCallbacks(Runnable action) { pending.remove(action); }
    }
}
