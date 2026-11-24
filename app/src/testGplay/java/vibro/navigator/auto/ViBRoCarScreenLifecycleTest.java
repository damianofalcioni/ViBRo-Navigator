package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.ComponentName;

import androidx.car.app.CarContext;
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

    private static final class RecordingScheduler implements TaskScheduler {
        final ArrayDeque<Runnable> pending = new ArrayDeque<>();

        @Override public void post(Runnable action) { pending.add(action); }
        @Override public void removeCallbacks(Runnable action) { pending.remove(action); }
    }
}
