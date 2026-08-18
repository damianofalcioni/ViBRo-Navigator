package vibro.navigator.android.automotive;

import static org.junit.Assert.assertEquals;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class AndroidCarModeMonitorTest {
    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AndroidCarModeMonitor.unregisterExitReceiver(context);
        AndroidCarModeMonitor.setActiveForTest(false);
    }

    @After
    public void tearDown() {
        AndroidCarModeMonitor.unregisterExitReceiver(context);
        AndroidCarModeMonitor.setActiveForTest(null);
    }

    @Test
    public void registerExitReceiverRunsCallbackOnCarModeExit() {
        AtomicInteger exitCallbacks = new AtomicInteger();
        AndroidCarModeMonitor.setActiveForTest(true);

        AndroidCarModeMonitor.registerExitReceiver(context, ignored -> exitCallbacks.incrementAndGet());
        AndroidCarModeMonitor.dispatchExitForTest(context);

        assertEquals(1, exitCallbacks.get());
    }

    @Test
    public void registerExitReceiverDoesNothingWhenCarModeIsInactive() {
        AtomicInteger exitCallbacks = new AtomicInteger();

        AndroidCarModeMonitor.registerExitReceiver(context, ignored -> exitCallbacks.incrementAndGet());
        AndroidCarModeMonitor.dispatchExitForTest(context);

        assertEquals(0, exitCallbacks.get());
    }
}
