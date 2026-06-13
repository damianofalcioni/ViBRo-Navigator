package vibro.navigator.nav.foreground;

import androidx.annotation.NonNull;

import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public interface NavigationForegroundController {

    void ensureChannels();

    void promoteToForeground(@NonNull NavigationRequest request, boolean paused);

    void stopForegroundService();

    boolean isOngoingNotificationVisible();

    void sendImminentTurnNotification(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds);

    void sendStationaryOrientationNotification(@NonNull StationaryOrientationAdvisor.Decision decision);

    void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice);

    void sendWrongDirectionNotification(@NonNull NavigationWrongDirectionNotice wrongDirectionNotice);
}
