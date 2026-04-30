package vibro.navigator.nav;

import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

final class NavigationServiceTurnNotificationSink implements NavigationTurnEventDispatcher.TurnNotificationSink {
    private final NavigationForegroundController foregroundController;

    NavigationServiceTurnNotificationSink(@NonNull NavigationForegroundController foregroundController) {
        this.foregroundController = foregroundController;
    }

    @Override
    public void sendImminentTurnNotification(
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        foregroundController.sendImminentTurnNotification(hint, distanceMeters, timeSeconds);
    }
}
