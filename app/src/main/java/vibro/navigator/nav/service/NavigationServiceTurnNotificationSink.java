package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import androidx.annotation.NonNull;

import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.voice.NavigationManeuverSpeaker;

public final class NavigationServiceTurnNotificationSink implements NavigationTurnEventDispatcher.TurnNotificationSink {
    private final NavigationForegroundController foregroundController;
    private final NavigationManeuverSpeaker maneuverSpeaker;

    public NavigationServiceTurnNotificationSink(
            @NonNull NavigationForegroundController foregroundController,
            @NonNull NavigationManeuverSpeaker maneuverSpeaker
    ) {
        this.foregroundController = foregroundController;
        this.maneuverSpeaker = maneuverSpeaker;
    }

    @Override
    public void sendImminentTurnNotification(
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        foregroundController.sendImminentTurnNotification(hint, distanceMeters, timeSeconds);
        maneuverSpeaker.speakTurn(hint, timeSeconds);
    }
}
