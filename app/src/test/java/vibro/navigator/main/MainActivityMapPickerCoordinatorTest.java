package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.map.MapPickerActivity;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class MainActivityMapPickerCoordinatorTest {

    private Activity activity;
    private RecordingScheduler scheduler;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        scheduler = new RecordingScheduler();
        activity.getSharedPreferences("vibenavigator_poi_history", Activity.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void openDestinationMapPicker_defersActivityLaunchForPressFeedback() {
        MainActivityMapPickerCoordinator coordinator = new MainActivityMapPickerCoordinator(activity, scheduler);
        PoiInputController controller = createPoiController(new EditText(activity));
        controller.setPoi(new Poi("Destination", 48.2082d, 16.3738d));

        coordinator.openDestinationMapPicker(controller);

        assertEquals(MainActivityMapPickerCoordinator.MAP_PICKER_LAUNCH_DELAY_MS, scheduler.delayMs);
        assertNull(shadowOf(activity).getNextStartedActivityForResult());

        scheduler.runDelayed();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertMapPickerStarted(started);
        controller.dispose();
    }

    @Test
    public void openStopMapPicker_defersActivityLaunchForPressFeedback() {
        MainActivityMapPickerCoordinator coordinator = new MainActivityMapPickerCoordinator(activity, scheduler);
        MainActivityStopController stopController = createStopController();
        stopController.addStopRow(null);
        PoiInputController controller = stopController.getStopControllers().get(0);
        controller.setPoi(new Poi("Stop", 48.2082d, 16.3738d));

        coordinator.openStopMapPicker(stopController, controller);

        assertEquals(MainActivityMapPickerCoordinator.MAP_PICKER_LAUNCH_DELAY_MS, scheduler.delayMs);
        assertNull(shadowOf(activity).getNextStartedActivityForResult());

        scheduler.runDelayed();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertMapPickerStarted(started);
        stopController.dispose();
    }

    @Test
    public void openStopMapPicker_ignoresRemovedStopBeforeDelayedLaunchRuns() {
        MainActivityMapPickerCoordinator coordinator = new MainActivityMapPickerCoordinator(activity, scheduler);
        MainActivityStopController stopController = createStopController();
        stopController.addStopRow(null);
        PoiInputController controller = stopController.getStopControllers().get(0);

        coordinator.openStopMapPicker(stopController, controller);
        stopController.dispose();

        scheduler.runDelayed();

        assertNull(shadowOf(activity).getNextStartedActivityForResult());
    }

    private MainActivityStopController createStopController() {
        return new MainActivityStopController(
                activity,
                new LinearLayout(activity),
                new PoiHistoryStore(activity),
                emptySearchClient(),
                stopInputController -> {
                }
        );
    }

    private PoiInputController createPoiController(EditText editText) {
        return new PoiInputController(
                activity,
                editText,
                new PoiHistoryStore(activity),
                emptySearchClient(),
                poi -> {
                }
        );
    }

    private static PoiSearchClient emptySearchClient() {
        return (query, limit) -> Collections.emptyList();
    }

    private static void assertMapPickerStarted(ShadowActivity.IntentForResult started) {
        assertNotNull(started);
        assertNotNull(started.intent.getComponent());
        assertEquals(MapPickerActivity.class.getName(), started.intent.getComponent().getClassName());
    }

    private static final class RecordingScheduler implements TaskScheduler {
        private Runnable delayedRunnable;
        private long delayMs = -1L;

        @Override
        public void post(@NonNull Runnable runnable) {
            delayedRunnable = runnable;
            delayMs = 0L;
        }

        @Override
        public void postDelayed(@NonNull Runnable runnable, long delayMs) {
            delayedRunnable = runnable;
            this.delayMs = delayMs;
        }

        private void runDelayed() {
            assertNotNull(delayedRunnable);
            Runnable runnable = delayedRunnable;
            delayedRunnable = null;
            runnable.run();
        }
    }
}
