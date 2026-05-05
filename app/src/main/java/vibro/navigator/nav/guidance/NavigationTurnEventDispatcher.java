package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.directions.VoiceHintMapper;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.logging.AppLogger;

import java.util.List;

public final class NavigationTurnEventDispatcher {

    public interface TurnNotificationSink {
        void sendImminentTurnNotification(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds);
    }

    private static final String TAG = "NavTurnEvents";

    private final TurnNotificationSink notificationSink;

    public NavigationTurnEventDispatcher(@NonNull TurnNotificationSink notificationSink) {
        this.notificationSink = notificationSink;
    }

    public void dispatch(@NonNull List<NavigationTurnEvent> turnEvents) {
        for (NavigationTurnEvent event : turnEvents) {
            switch (event.type) {
                case PASSED:
                    AppLogger.i(TAG, "Passed voice hint hintTrackIndex=" + event.hint.indexInTrack);
                    break;
                case INITIAL:
                    AppLogger.i(TAG, "Sent initial turn notification distanceMeters=" + event.distanceMeters
                            + " timeSeconds=" + event.timeSeconds);
                    notifyImminent(event.hint, event.distanceMeters, event.timeSeconds);
                    break;
                case IMMINENT:
                    notifyImminent(event.hint, event.distanceMeters, event.timeSeconds);
                    break;
            }
        }
    }

    private void notifyImminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
        DirectionInfo directionInfo = VoiceHintMapper.toDirection(hint);
        AppLogger.i(TAG, "Imminent turn kind=" + directionInfo.kind
                + " distanceMeters=" + distanceMeters
                + " timeSeconds=" + timeSeconds);
        notificationSink.sendImminentTurnNotification(hint, distanceMeters, timeSeconds);
    }
}
