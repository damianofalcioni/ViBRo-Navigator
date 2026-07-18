package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TurnEventPlanner {

    public static final class Progress {
        final int nextHintIdx;
        final boolean notified20;
        final boolean notified5;
        final boolean advancedPastInstruction;
        @NonNull
        final List<TurnSignal> signals;

        private Progress(
                int nextHintIdx,
                boolean notified20,
                boolean notified5,
                boolean advancedPastInstruction,
                @NonNull List<TurnSignal> signals
        ) {
            this.nextHintIdx = nextHintIdx;
            this.notified20 = notified20;
            this.notified5 = notified5;
            this.advancedPastInstruction = advancedPastInstruction;
            this.signals = signals;
        }
    }

    public static final class TurnSignal {
        public enum Type {
            PASSED,
            IMMINENT,
            INITIAL
        }

        @NonNull
        final Type type;
        @NonNull
        final VoiceHint hint;
        final double distanceMeters;
        final double timeSeconds;

        private TurnSignal(@NonNull Type type, @NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.type = type;
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }

        @NonNull
        public static TurnSignal passed(@NonNull VoiceHint hint) {
            return new TurnSignal(Type.PASSED, hint, 0.0, 0.0);
        }

        @NonNull
        public static TurnSignal imminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnSignal(Type.IMMINENT, hint, distanceMeters, timeSeconds);
        }

        @NonNull
        public static TurnSignal initial(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnSignal(Type.INITIAL, hint, distanceMeters, timeSeconds);
        }
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps
    ) {
        return advance(
                route,
                polylineIndex,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                false
        );
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            boolean singleInstructionMode
    ) {
        return advance(
                route,
                polylineIndex,
                route.voiceHints,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                singleInstructionMode
        );
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps
    ) {
        return advance(
                route,
                polylineIndex,
                hints,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                false
        );
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            boolean singleInstructionMode
    ) {
        return advance(
                route,
                polylineIndex,
                hints,
                buildHintAlongTrackMeters(polylineIndex, hints),
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                singleInstructionMode
        );
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps
    ) {
        return advance(
                route,
                polylineIndex,
                hints,
                hintAlongTrackMeters,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                false
        );
    }

    @NonNull
    public Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            boolean singleInstructionMode
    ) {
        return advance(
                route,
                polylineIndex,
                hints,
                hintAlongTrackMeters,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                RouteMotionEstimate.speedOnly(speedMps),
                TurnNotificationPlan.from(singleInstructionMode)
        );
    }

    @NonNull
    Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accelerationMps2,
            @NonNull TurnNotificationPlan notificationPlan
    ) {
        return advance(
                route,
                polylineIndex,
                hints,
                hintAlongTrackMeters,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                RouteMotionEstimate.withAcceleration(speedMps, accelerationMps2),
                notificationPlan
        );
    }

    @NonNull
    private Progress advance(
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
            @NonNull TurnNotificationPlan notificationPlan
    ) {
        if (hints.isEmpty() || nextHintIdx >= hints.size()) {
            return new Progress(nextHintIdx, notified20, notified5, false, Collections.emptyList());
        }

        List<TurnSignal> signals = new ArrayList<>();
        TurnHintAdvancePolicy.Result consumed = TurnHintAdvancePolicy.consumePassedAndRetiredHints(
                route,
                polylineIndex,
                hints,
                hintAlongTrackMeters,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                motionEstimate,
                signals
        );
        AdvanceCursor cursor = new AdvanceCursor(
                consumed.nextHintIdx,
                consumed.notified20,
                consumed.notified5,
                consumed.advancedPastInstruction
        );

        if (cursor.nextHintIdx >= hints.size()) {
            return cursor.toProgress(signals);
        }

        return appendImminentSignalIfNeeded(
                route,
                polylineIndex,
                hints,
                hintAlongTrackMeters,
                cursor,
                alongTrackMeters,
                currentSegmentIndex,
                motionEstimate,
                notificationPlan,
                signals
        );
    }

    @NonNull
    private Progress appendImminentSignalIfNeeded(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            @NonNull AdvanceCursor cursor,
            double alongTrackMeters,
            int currentSegmentIndex,
            @NonNull RouteMotionEstimate motionEstimate,
            @NonNull TurnNotificationPlan notificationPlan,
            @NonNull List<TurnSignal> signals
    ) {
        VoiceHint next = hints.get(cursor.nextHintIdx);
        double hintDistMeters = hintAlongTrackMeters.get(cursor.nextHintIdx);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                hintDistMeters,
                motionEstimate
        );
        if (timeToNextSeconds == null) {
            return cursor.toProgress(signals);
        }
        if (!TurnDistanceReliability.isImminentReliable(
                distanceToNextMeters,
                motionEstimate.speedMps,
                timeToNextSeconds
        )) {
            return cursor.toProgress(signals);
        }

        boolean updatedNotified20 = cursor.notified20;
        boolean updatedNotified5 = cursor.notified5;
        if (!cursor.notified5 && notificationPlan.isClosestAlertDue(timeToNextSeconds)) {
            updatedNotified20 = true;
            updatedNotified5 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
            return new Progress(
                    cursor.nextHintIdx,
                    updatedNotified20,
                    updatedNotified5,
                    cursor.advancedPastInstruction,
                    signals
            );
        }
        if (!cursor.notified20 && notificationPlan.isPreparatoryAlertDue(timeToNextSeconds)) {
            updatedNotified20 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
        }
        return new Progress(
                cursor.nextHintIdx,
                updatedNotified20,
                updatedNotified5,
                cursor.advancedPastInstruction,
                signals
        );
    }

    private static final class AdvanceCursor {
        public final int nextHintIdx;
        public final boolean notified20;
        public final boolean notified5;
        public final boolean advancedPastInstruction;

        private AdvanceCursor(
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

        @NonNull
        public Progress toProgress(@NonNull List<TurnSignal> signals) {
            return new Progress(nextHintIdx, notified20, notified5, advancedPastInstruction, signals);
        }
    }

    @Nullable
    public TurnSignal buildInitialSignal(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean initialTurnNotificationSent,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        return buildInitialSignal(
                route,
                polylineIndex,
                route.voiceHints,
                nextHintIdx,
                initialTurnNotificationSent,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
    }

    @Nullable
    public TurnSignal buildInitialSignal(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            int nextHintIdx,
            boolean initialTurnNotificationSent,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        return buildInitialSignal(
                route,
                polylineIndex,
                hints,
                buildHintAlongTrackMeters(polylineIndex, hints),
                nextHintIdx,
                initialTurnNotificationSent,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
    }

    @Nullable
    public TurnSignal buildInitialSignal(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean initialTurnNotificationSent,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        if (initialTurnNotificationSent
                || hints.isEmpty()
                || nextHintIdx < 0
                || nextHintIdx >= hints.size()) {
            return null;
        }

        VoiceHint next = hints.get(nextHintIdx);
        double hintDistMeters = hintAlongTrackMeters.get(nextHintIdx);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        if (!TurnDistanceReliability.isInitialReliable(distanceToNextMeters, accuracyMeters)) {
            return null;
        }
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                hintDistMeters,
                speedMps
        );
        return TurnSignal.initial(
                next,
                distanceToNextMeters,
                timeToNextSeconds != null ? timeToNextSeconds : Double.NaN
        );
    }

    @NonNull
    private static List<Double> buildHintAlongTrackMeters(
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<VoiceHint> hints
    ) {
        List<Double> hintDistances = new ArrayList<>(hints.size());
        for (VoiceHint hint : hints) {
            hintDistances.add(polylineIndex.distanceAtPointIndex(hint.indexInTrack));
        }
        return hintDistances;
    }

}
