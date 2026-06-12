package vibro.navigator.about;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.widget.Button;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class AboutDeferredDialogActionTest {

    @Test
    public void attachTo_defersActionUntilPressFeedbackDelayRuns() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Button button = new Button(activity);
        RecordingScheduler scheduler = new RecordingScheduler();
        AtomicInteger openCount = new AtomicInteger();
        new AboutDeferredDialogAction(activity, scheduler, openCount::incrementAndGet).attachTo(button);

        button.performClick();
        button.performClick();

        assertEquals(0, openCount.get());
        assertEquals(1, scheduler.scheduleCount);
        assertEquals(AboutDeferredDialogAction.OPEN_DELAY_MS, scheduler.delayMs);

        scheduler.runDelayed();

        assertEquals(1, openCount.get());
    }

    @Test
    public void attachTo_skipsActionWhenActivityIsFinishingBeforeDelayRuns() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Button button = new Button(activity);
        RecordingScheduler scheduler = new RecordingScheduler();
        AtomicInteger openCount = new AtomicInteger();
        new AboutDeferredDialogAction(activity, scheduler, openCount::incrementAndGet).attachTo(button);

        button.performClick();
        activity.finish();
        scheduler.runDelayed();

        assertEquals(0, openCount.get());
    }

    private static final class RecordingScheduler implements TaskScheduler {
        private Runnable delayedRunnable;
        private long delayMs = -1L;
        private int scheduleCount;

        @Override
        public void post(@NonNull Runnable runnable) {
            delayedRunnable = runnable;
            delayMs = 0L;
            scheduleCount++;
        }

        @Override
        public void postDelayed(@NonNull Runnable runnable, long delayMs) {
            delayedRunnable = runnable;
            this.delayMs = delayMs;
            scheduleCount++;
        }

        private void runDelayed() {
            delayedRunnable.run();
        }
    }
}
