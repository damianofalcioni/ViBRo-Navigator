package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;

public final class NavigationServiceTurnNotificationSink implements NavigationTurnEventDispatcher.TurnNotificationSink {
    private final NavigationForegroundController foregroundController;

    public NavigationServiceTurnNotificationSink(@NonNull NavigationForegroundController foregroundController) {
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
