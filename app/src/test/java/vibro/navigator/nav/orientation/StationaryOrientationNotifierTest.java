package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.hardware.SensorManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class StationaryOrientationNotifierTest {

    private final StationaryOrientationNotifier notifier =
            new StationaryOrientationNotifier(new StationaryOrientationAdvisor());
    private final RecordingSink sink = new RecordingSink();

    @Test
    public void maybeNotifySendsOncePerStationaryStop() {
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                90.0,
                sample(20.0, 1_000L),
                1_000L,
                sink
        );
        assertEquals(0, sink.decisions.size());

        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                90.0,
                sample(20.0, 6_000L),
                6_500L,
                sink
        );
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                90.0,
                sample(20.0, 7_000L),
                7_000L,
                sink
        );

        assertEquals(1, sink.decisions.size());
        assertEquals(70.0, sink.decisions.get(0).absoluteTurnDegrees(), 0.001);
        assertNotNull(notifier.activeOrientationCue());
        assertEquals(90.0f, notifier.activeOrientationCue().targetHeadingDegrees, 0.001f);
    }

    @Test
    public void maybeNotifyAllowsNewNotificationAfterMovementReset() {
        notifyAfterDwell(1_000L, 6_500L);
        notifier.maybeNotify(
                true,
                false,
                false,
                1.2f,
                90.0,
                sample(20.0, 7_000L),
                7_000L,
                sink
        );
        assertNull(notifier.activeOrientationCue());
        notifyAfterDwell(8_000L, 13_500L);

        assertEquals(2, sink.decisions.size());
    }

    @Test
    public void maybeNotifyClearsCueWhenMovementStartsEvenIfNotAlignedWithRoute() {
        notifyAfterDwell(1_000L, 6_500L);

        notifier.maybeNotify(
                true,
                false,
                false,
                1.2f,
                90.0,
                sample(20.0, 7_000L),
                7_000L,
                sink
        );

        assertNull(notifier.activeOrientationCue());
    }

    @Test
    public void maybeNotifyDoesNotExposeCueWhenAlreadyAligned() {
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                25.0,
                sample(20.0, 1_000L),
                1_000L,
                sink
        );
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                25.0,
                sample(20.0, 6_000L),
                6_500L,
                sink
        );
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                25.0,
                sample(20.0, 7_000L),
                7_000L,
                sink
        );

        assertEquals(0, sink.decisions.size());
        assertNull(notifier.activeOrientationCue());
    }

    private void notifyAfterDwell(long startMs, long notifyMs) {
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                90.0,
                sample(20.0, startMs),
                startMs,
                sink
        );
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                90.0,
                sample(20.0, notifyMs),
                notifyMs,
                sink
        );
    }

    private static GeomagneticOrientationMonitor.Sample sample(double headingDegrees, long elapsedRealtimeMs) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                3.0,
                elapsedRealtimeMs
        );
    }

    private static final class RecordingSink implements StationaryOrientationNotifier.Sink {
        private final List<StationaryOrientationAdvisor.Decision> decisions = new ArrayList<>();

        @Override
        public void sendStationaryOrientationNotification(
                StationaryOrientationAdvisor.Decision decision
        ) {
            decisions.add(decision);
        }
    }
}
