package vibro.navigator.nav.voice;

import androidx.annotation.NonNull;

import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public interface NavigationAlertSpeaker {
    void speakTurn(@NonNull VoiceHint hint, double timeSeconds);

    void speakStationaryOrientation(@NonNull StationaryOrientationAdvisor.Decision decision);

    void speakOffRoute(@NonNull NavigationRerouteNotice rerouteNotice);

    void speakWrongDirection();
}
