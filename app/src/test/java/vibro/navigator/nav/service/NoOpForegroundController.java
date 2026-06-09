package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

final class NoOpForegroundController implements NavigationForegroundController {
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
        return false;
    }

    @Override
    public void sendImminentTurnNotification(
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
    }

    @Override
    public void sendStationaryOrientationNotification(
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
    }

    @Override
    public void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
    }
}
