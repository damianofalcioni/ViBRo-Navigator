package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.nav.route.VoiceHint;

final class TurnHintAdvancePolicy {
    private static final double PASSED_HINT_BUFFER_METERS = 5.0;

    private TurnHintAdvancePolicy() {
    }

    @NonNull
    static Result consumePassedAndRetiredHints(
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
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
                    hintAlongTrackMetersValue,
                    alongTrackMeters,
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
            double hintAlongTrackMeters,
            double alongTrackMeters,
            boolean notified5
    ) {
        return notified5 && alongTrackMeters >= hintAlongTrackMeters;
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
