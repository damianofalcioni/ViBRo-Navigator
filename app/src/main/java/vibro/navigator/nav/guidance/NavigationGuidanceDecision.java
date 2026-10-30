package vibro.navigator.nav.guidance;

import java.util.List;
import java.util.Locale;
import vibro.navigator.nav.route.VoiceHint;

/** The planner's actual target and outcome; diagnostics must not re-run alert policy. */
final class NavigationGuidanceDecision {
    enum Reason {
        UNTRUSTED_PROGRESS, NO_HINT, ARRIVAL_UNCONFIRMED, ETA_UNAVAILABLE,
        DISTANCE_UNRELIABLE, NOT_DUE, ALREADY_SENT, CLOSEST_ALERT, PREPARATORY_ALERT
    }

    final int hintTrackIndex;
    final int command;
    final double distanceMeters;
    final double timeSeconds;
    final Reason reason;

    private NavigationGuidanceDecision(int hintTrackIndex, int command, double distanceMeters,
            double timeSeconds, Reason reason) {
        this.hintTrackIndex = hintTrackIndex;
        this.command = command;
        this.distanceMeters = distanceMeters;
        this.timeSeconds = timeSeconds;
        this.reason = reason;
    }

    static NavigationGuidanceDecision pending(List<VoiceHint> hints, List<Double> distances, int index, double along) {
        if (index >= hints.size()) {
            return new NavigationGuidanceDecision(-1, -1, Double.NaN, Double.NaN, Reason.NO_HINT);
        }
        VoiceHint hint = hints.get(index);
        return new NavigationGuidanceDecision(hint.indexInTrack, hint.command,
                Math.max(0, distances.get(index) - along), Double.NaN,
                NavigationTurnManeuverCueState.isSyntheticArrivalHint(hint)
                        ? Reason.ARRIVAL_UNCONFIRMED : Reason.UNTRUSTED_PROGRESS);
    }

    NavigationGuidanceDecision withTime(Double seconds, Reason outcome) {
        return new NavigationGuidanceDecision(hintTrackIndex, command, distanceMeters,
                seconds != null ? seconds : Double.NaN, outcome);
    }

    static Reason pendingAlertReason(boolean closestDue, boolean preparatoryDue, boolean closestSent, boolean preparatorySent) {
        return (closestDue && closestSent) || (preparatoryDue && preparatorySent) ? Reason.ALREADY_SENT : Reason.NOT_DUE;
    }

    String reasonText() {
        return reason.name().toLowerCase(Locale.ROOT);
    }
}
