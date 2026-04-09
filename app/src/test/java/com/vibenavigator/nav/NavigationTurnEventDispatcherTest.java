package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationTurnEventDispatcherTest {

    @Test
    public void passedEventDoesNotSendNotification() {
        RecordingSink sink = new RecordingSink();
        NavigationTurnEventDispatcher dispatcher = new NavigationTurnEventDispatcher(sink);

        dispatcher.dispatch(Collections.singletonList(
                NavigationSession.TurnEvent.passed(new VoiceHint(7, 2, 0, 0.0, 0))
        ));

        assertEquals(0, sink.imminentCalls);
    }

    @Test
    public void initialAndImminentEventsUseImminentNotification() {
        RecordingSink sink = new RecordingSink();
        NavigationTurnEventDispatcher dispatcher = new NavigationTurnEventDispatcher(sink);

        dispatcher.dispatch(Arrays.asList(
                NavigationSession.TurnEvent.initial(new VoiceHint(3, 2, 0, 0.0, 0), 120.0, 10.0),
                NavigationSession.TurnEvent.imminent(new VoiceHint(4, 5, 0, 0.0, 0), 25.0, 2.0)
        ));

        assertEquals(2, sink.imminentCalls);
        assertEquals(4, sink.lastImminentHintIndex);
        assertEquals(25.0, sink.lastImminentDistanceMeters, 0.0);
        assertEquals(2.0, sink.lastImminentTimeSeconds, 0.0);
    }

    private static final class RecordingSink implements NavigationTurnEventDispatcher.TurnNotificationSink {
        int imminentCalls;
        int lastImminentHintIndex = -1;
        double lastImminentDistanceMeters;
        double lastImminentTimeSeconds;

        @Override
        public void sendImminentTurnNotification(VoiceHint hint, double distanceMeters, double timeSeconds) {
            imminentCalls++;
            lastImminentHintIndex = hint.indexInTrack;
            lastImminentDistanceMeters = distanceMeters;
            lastImminentTimeSeconds = timeSeconds;
        }
    }
}
