package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.List;

final class NavigationTurnManeuverCueState {
    private static final double TURN_CUE_NOTIFICATION_THRESHOLD_SECONDS = 5.0;

    @Nullable
    private Integer activeTurnManeuverDegrees;
    @Nullable
    private Integer activeTurnManeuverTrackIndex;

    void clear() {
        activeTurnManeuverDegrees = null;
        activeTurnManeuverTrackIndex = null;
    }

    @Nullable
    Integer activeTurnManeuverDegrees() {
        return activeTurnManeuverDegrees;
    }

    @Nullable
    Integer activeTurnManeuverTrackIndex() {
        return activeTurnManeuverTrackIndex;
    }

    void update(@NonNull List<TurnEventPlanner.TurnSignal> signals) {
        for (TurnEventPlanner.TurnSignal signal : signals) {
            update(signal);
        }
    }

    void clearIfPassed(@NonNull PolylineIndex polylineIndex, double alongTrackMeters) {
        if (activeTurnManeuverTrackIndex == null) {
            return;
        }
        if (alongTrackMeters >= polylineIndex.distanceAtPointIndex(activeTurnManeuverTrackIndex)) {
            clear();
        }
    }

    @NonNull
    static List<NavigationTurnEvent> destinationArrival(int trackIndex) {
        return NavigationArrivalTurnEvents.destinationArrival(trackIndex);
    }

    @NonNull
    static List<NavigationTurnEvent> intermediateArrival(int trackIndex) {
        return NavigationArrivalTurnEvents.intermediateArrival(trackIndex);
    }

    private void update(@NonNull TurnEventPlanner.TurnSignal signal) {
        if (signal.type == TurnEventPlanner.TurnSignal.Type.PASSED) {
            clear();
            return;
        }
        if (isClosestTurnNotification(signal)) {
            activeTurnManeuverDegrees = signal.hint.angleDegrees;
            activeTurnManeuverTrackIndex = signal.hint.indexInTrack;
        }
    }

    private static boolean isClosestTurnNotification(@NonNull TurnEventPlanner.TurnSignal signal) {
        return signal.type == TurnEventPlanner.TurnSignal.Type.IMMINENT
                && signal.timeSeconds <= TURN_CUE_NOTIFICATION_THRESHOLD_SECONDS
                && Double.isFinite(signal.timeSeconds)
                && !isSyntheticArrivalHint(signal.hint);
    }

    private static boolean isSyntheticArrivalHint(@NonNull VoiceHint hint) {
        return hint.command == NavigationArrivalTurnEvents.DESTINATION_ARRIVAL_COMMAND
                || hint.command == NavigationArrivalTurnEvents.INTERMEDIATE_ARRIVAL_COMMAND;
    }
}
