package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.service.NavigationServiceTurnNotificationSink;

public class NavigationSpeechForegroundControllerTest {

    @Test
    public void progressConfidenceSuppressionAndRecoveryKeepSpeechAndNotificationsTogether() {
        RecordingForegroundController foregroundController = new RecordingForegroundController();
        RecordingAlertSpeaker speaker = new RecordingAlertSpeaker();
        NavigationSpeechForegroundController controller =
                new NavigationSpeechForegroundController(foregroundController, speaker);
        NavigationTurnEventDispatcher dispatcher = new NavigationTurnEventDispatcher(
                new NavigationServiceTurnNotificationSink(controller));
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 1, 0, 0.0, 0)), 77.0, 111.0);
        PolylineIndex index = new PolylineIndex(route.track);
        state.onRouteApplied(route, index, Collections.emptyList(), new LatLon(0.0, 0.0), 1.45f, 4f);
        double alongTrackMeters = index.totalLengthMeters() - 7.0;

        dispatcher.dispatch(state.evaluate(route, index, alongTrackMeters,
                0, 1.45f, Float.NaN, false, 1_000L, 0L, false).turnEvents);
        assertEquals(0, foregroundController.turnNotifications);
        assertEquals(0, speaker.turnSpeechCalls);

        dispatcher.dispatch(state.evaluate(route, index, alongTrackMeters,
                0, 1.45f, Float.NaN, true, 4_000L, 0L, false).turnEvents);
        assertEquals(1, foregroundController.turnNotifications);
        assertEquals(1, speaker.turnSpeechCalls);
        assertEquals(foregroundController.lastTurnHint, speaker.lastTurnHint);
        assertEquals(foregroundController.lastTurnTimeSeconds, speaker.lastTurnSpeechTimeSeconds, 0.0);

        dispatcher.dispatch(state.evaluate(route, index, alongTrackMeters,
                0, 1.45f, Float.NaN, true, 7_000L, 0L, false).turnEvents);
        assertEquals(1, foregroundController.turnNotifications);
        assertEquals(1, speaker.turnSpeechCalls);
    }

    @Test
    public void sendAlertNotificationsDelegatesAndSpeaksMatchingAlert() {
        RecordingForegroundController foregroundController = new RecordingForegroundController();
        RecordingAlertSpeaker speaker = new RecordingAlertSpeaker();
        NavigationSpeechForegroundController controller =
                new NavigationSpeechForegroundController(foregroundController, speaker);
        NavigationRerouteNotice rerouteNotice = NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(25.0, 8f, 90.0, 90.0)
        );

        controller.sendImminentTurnNotification(new VoiceHint(0, 2, 0, 0.0, 0), 50.0, 5.0);
        controller.sendStationaryOrientationNotification(new StationaryOrientationAdvisor.Decision(-42.0));
        controller.sendOffRouteNotification(rerouteNotice);
        controller.sendWrongDirectionNotification(new NavigationWrongDirectionNotice(90.0, 270.0, 180.0));

        assertEquals(1, foregroundController.turnNotifications);
        assertEquals(1, foregroundController.stationaryOrientationNotifications);
        assertEquals(1, foregroundController.offRouteNotifications);
        assertEquals(1, foregroundController.wrongDirectionNotifications);
        assertEquals(1, speaker.turnSpeechCalls);
        assertEquals(5.0, speaker.lastTurnSpeechTimeSeconds, 0.0);
        assertEquals(1, speaker.stationaryOrientationSpeechCalls);
        assertEquals(1, speaker.offRouteSpeechCalls);
        assertEquals(1, speaker.wrongDirectionSpeechCalls);
    }

    private static final class RecordingForegroundController implements NavigationForegroundController {
        int turnNotifications;
        VoiceHint lastTurnHint;
        double lastTurnTimeSeconds;
        int stationaryOrientationNotifications;
        int offRouteNotifications;
        int wrongDirectionNotifications;

        @Override
        public void ensureChannels() {
        }

        @Override
        public void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        }

        @Override
        public void stopForegroundService() {
        }

        @Override
        public boolean isOngoingNotificationVisible() {
            return true;
        }

        @Override
        public void sendImminentTurnNotification(
                @NonNull VoiceHint hint,
                double distanceMeters,
                double timeSeconds
        ) {
            turnNotifications++;
            lastTurnHint = hint;
            lastTurnTimeSeconds = timeSeconds;
        }

        @Override
        public void sendStationaryOrientationNotification(
                @NonNull StationaryOrientationAdvisor.Decision decision
        ) {
            stationaryOrientationNotifications++;
        }

        @Override
        public void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
            offRouteNotifications++;
        }

        @Override
        public void sendWrongDirectionNotification(@NonNull NavigationWrongDirectionNotice wrongDirectionNotice) {
            wrongDirectionNotifications++;
        }
    }

    private static final class RecordingAlertSpeaker implements NavigationAlertSpeaker {
        int turnSpeechCalls;
        VoiceHint lastTurnHint;
        double lastTurnSpeechTimeSeconds;
        int stationaryOrientationSpeechCalls;
        int offRouteSpeechCalls;
        int wrongDirectionSpeechCalls;

        @Override
        public void speakTurn(@NonNull VoiceHint hint, double timeSeconds) {
            turnSpeechCalls++;
            lastTurnHint = hint;
            lastTurnSpeechTimeSeconds = timeSeconds;
        }

        @Override
        public void speakStationaryOrientation(@NonNull StationaryOrientationAdvisor.Decision decision) {
            stationaryOrientationSpeechCalls++;
        }

        @Override
        public void speakOffRoute(@NonNull NavigationRerouteNotice rerouteNotice) {
            offRouteSpeechCalls++;
        }

        @Override
        public void speakWrongDirection() {
            wrongDirectionSpeechCalls++;
        }
    }
}
