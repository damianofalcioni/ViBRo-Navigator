package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

final class TurnHintAdvancePolicy {
    private static final double PASSED_HINT_BUFFER_METERS = 5.0;

    private TurnHintAdvancePolicy() {
    }

    @NonNull
    static Result consumePassedAndRetiredHints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            @NonNull RouteMotionEstimate motionEstimate,
            @NonNull List<TurnEventPlanner.TurnSignal> signals
    ) {
        int updatedHintIdx = nextHintIdx;
        boolean updatedNotified20 = notified20;
        boolean updatedNotified5 = notified5;
        boolean advancedPastInstruction = false;
        while (updatedHintIdx < hints.size()) {
            double hintAlongTrackMetersValue = hintAlongTrackMeters.get(updatedHintIdx);
            boolean passed = hasPassedHint(hintAlongTrackMetersValue, alongTrackMeters);
            boolean retired = shouldRetireAlreadyNotifiedHint(
                    route,
                    polylineIndex,
                    hintAlongTrackMetersValue,
                    alongTrackMeters,
                    currentSegmentIndex,
                    motionEstimate,
                    updatedNotified5
            );
            if (!passed && !retired) {
                break;
            }
            if (passed) {
                signals.add(TurnEventPlanner.TurnSignal.passed(hints.get(updatedHintIdx)));
            }
            advancedPastInstruction = true;
            updatedHintIdx++;
            updatedNotified20 = false;
            updatedNotified5 = false;
        }
        return new Result(updatedHintIdx, updatedNotified20, updatedNotified5, advancedPastInstruction);
    }

    private static boolean hasPassedHint(
            double hintAlongTrackMeters,
            double alongTrackMeters
    ) {
        return alongTrackMeters >= hintAlongTrackMeters + PASSED_HINT_BUFFER_METERS;
    }

    private static boolean shouldRetireAlreadyNotifiedHint(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double hintAlongTrackMeters,
            double alongTrackMeters,
            int currentSegmentIndex,
            @NonNull RouteMotionEstimate motionEstimate,
            boolean notified5
    ) {
        if (!notified5) {
            return false;
        }
        double distanceToHintMeters = hintAlongTrackMeters - alongTrackMeters;
        if (distanceToHintMeters <= 0.0) {
            return true;
        }
        Double timeToHintSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                hintAlongTrackMeters,
                motionEstimate
        );
        return timeToHintSeconds != null
                && Double.isFinite(timeToHintSeconds)
                && timeToHintSeconds <= TurnDistanceReliability.MIN_ACTIONABLE_NOTICE_SECONDS;
    }

    static final class Result {
        final int nextHintIdx;
        final boolean notified20;
        final boolean notified5;
        final boolean advancedPastInstruction;

        private Result(
                int nextHintIdx,
                boolean notified20,
                boolean notified5,
                boolean advancedPastInstruction
        ) {
            this.nextHintIdx = nextHintIdx;
            this.notified20 = notified20;
            this.notified5 = notified5;
            this.advancedPastInstruction = advancedPastInstruction;
        }
    }
}
