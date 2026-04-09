package com.vibenavigator.nav;

import androidx.annotation.NonNull;

import com.vibenavigator.nav.directions.DirectionInfo;
import com.vibenavigator.nav.directions.VoiceHintMapper;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.List;

final class NavigationTurnEventDispatcher {

    interface TurnNotificationSink {
        void sendImminentTurnNotification(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds);
    }

    private static final String TAG = "NavTurnEvents";

    private final TurnNotificationSink notificationSink;

    NavigationTurnEventDispatcher(@NonNull TurnNotificationSink notificationSink) {
        this.notificationSink = notificationSink;
    }

    void dispatch(@NonNull List<NavigationSession.TurnEvent> turnEvents) {
        for (NavigationSession.TurnEvent event : turnEvents) {
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
