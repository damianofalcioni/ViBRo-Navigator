package vibro.navigator.nav.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Observed fixes translated to local metres and relative time; no absolute location is stored. */
public class StationaryDeskLogReplayTest {
    @Test
    public void recordedSpeedlessTenSecondFusedFixesEnterSleepAndRepeatFallbackCycles() throws IOException {
        List<Observation> observations = observations();
        Replay replay = new Replay();
        for (Observation observation : observations) {
            replay.accept(observation);
        }
        assertEquals(42, observations.size());
        assertEquals(observations.get(3).callbackMs, replay.firstSleepMs);
        assertTrue(replay.sleeps >= 5);
        assertTrue(replay.checks >= 4);
        assertEquals(2, replay.trigger.registrations);
        assertTrue(replay.decisions.stream().anyMatch(message -> message.contains("reason=callback_gap")));
        assertTrue(replay.decisions.stream().anyMatch(message -> message.contains("reason=ready")
                && message.contains("nativeSpeed=unknown")));
    }

    @Test
    public void sameLogCannotSleepWhenWakeUpSensorCannotBeArmed() throws IOException {
        Trigger trigger = new Trigger();
        trigger.available = false;
        NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> { });
        for (Observation observation : observations()) {
            assertFalse(sleep.observe(observation.raw, observation.filtered, true, observation.callbackMs));
        }
    }

    private static List<Observation> observations() throws IOException {
        var stream = Objects.requireNonNull(StationaryDeskLogReplayTest.class.getResourceAsStream(
                "/navigation/stationary-desk-20261023.csv"));
        List<Observation> observations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                observations.add(new Observation(line.split(",")));
            }
        }
        return observations;
    }

    private static final class Observation {
        private final NavigationLocation raw;
        private final NavigationLocation filtered;
        private final long callbackMs;

        Observation(String[] fields) {
            long fixMs = Long.parseLong(fields[0]);
            callbackMs = Long.parseLong(fields[1]);
            raw = location(fixMs, fields, 2);
            filtered = location(fixMs, fields, 5);
        }

        private static NavigationLocation location(long time, String[] fields, int offset) {
            NavigationLocation location = new NavigationLocation("fused");
            location.setTime(time, time);
            location.setLongitude(Double.parseDouble(fields[offset]) / 111_195d);
            location.setLatitude(Double.parseDouble(fields[offset + 1]) / 111_195d);
            location.setAccuracy(Float.parseFloat(fields[offset + 2]));
            return location;
        }
    }

    private static final class Replay {
        private final Trigger trigger = new Trigger();
        private final List<String> decisions = new ArrayList<>();
        private final NavigationStationarySleep sleep = new NavigationStationarySleep(trigger, () -> { }, decisions::add);
        private int sleeps;
        private int checks;
        private long firstSleepMs = -1L;

        void accept(Observation observation) {
            if (sleep.isSleeping()) {
                if (observation.callbackMs < sleep.deadlineMs()) {
                    return;
                }
                sleep.beginCheck();
                checks++;
            }
            if (sleep.observe(observation.raw, observation.filtered, true, observation.callbackMs)) {
                if (firstSleepMs < 0L) {
                    firstSleepMs = observation.callbackMs;
                }
                sleeps++;
                sleep.beginSleep(observation.callbackMs);
            }
        }
    }

    private static final class Trigger implements NavigationMotionTrigger {
        private boolean available = true;
        private int registrations;
        @Override public boolean arm(Runnable onMotion) { registrations++; return available; }
        @Override public void cancel() { }
    }
}
