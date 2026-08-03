package vibro.navigator.nav.voice;

import androidx.annotation.NonNull;

import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public final class NavigationSpeechForegroundController implements NavigationForegroundController {
    @NonNull
    private final NavigationForegroundController delegate;
    @NonNull
    private final NavigationAlertSpeaker speaker;

    public NavigationSpeechForegroundController(
            @NonNull NavigationForegroundController delegate,
            @NonNull NavigationAlertSpeaker speaker
    ) {
        this.delegate = delegate;
        this.speaker = speaker;
    }

    @Override
    public void ensureChannels() {
        delegate.ensureChannels();
    }

    @Override
    public void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        delegate.promoteToForeground(request, paused);
    }

    @Override
    public void stopForegroundService() {
        delegate.stopForegroundService();
    }

    @Override
    public boolean isOngoingNotificationVisible() {
        return delegate.isOngoingNotificationVisible();
    }

    @Override
    public void sendImminentTurnNotification(
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        delegate.sendImminentTurnNotification(hint, distanceMeters, timeSeconds);
        speaker.speakTurn(hint, timeSeconds);
    }

    @Override
    public void sendStationaryOrientationNotification(
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        delegate.sendStationaryOrientationNotification(decision);
        speaker.speakStationaryOrientation(decision);
    }

    @Override
    public void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
        delegate.sendOffRouteNotification(rerouteNotice);
        speaker.speakOffRoute(rerouteNotice);
    }

    @Override
    public void sendWrongDirectionNotification(@NonNull NavigationWrongDirectionNotice wrongDirectionNotice) {
        delegate.sendWrongDirectionNotification(wrongDirectionNotice);
        speaker.speakWrongDirection();
    }
}
