package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationSpeechForegroundControllerTest {

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
        double lastTurnSpeechTimeSeconds;
        int stationaryOrientationSpeechCalls;
        int offRouteSpeechCalls;
        int wrongDirectionSpeechCalls;

        @Override
        public void speakTurn(@NonNull VoiceHint hint, double timeSeconds) {
            turnSpeechCalls++;
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
