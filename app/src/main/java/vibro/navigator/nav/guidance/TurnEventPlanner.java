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

    private static final double PASSED_HINT_BUFFER_METERS = 5.0;
    private static final double PREPARATORY_IMMINENT_THRESHOLD_SECONDS = 20.0;
    private static final double VERY_IMMINENT_THRESHOLD_SECONDS = 5.0;
    private static final double MIN_TRUSTED_TURN_DISTANCE_METERS = 5.0;
    private static final double MIN_SLOW_SPEED_TURN_DISTANCE_METERS = 0.75;
    private static final double MIN_ACTIONABLE_NOTICE_SECONDS = 2.0;

    public static final class Progress {
        final int nextHintIdx;
        final boolean notified20;
        final boolean notified5;
        @NonNull
        final List<TurnSignal> signals;

        private Progress(int nextHintIdx, boolean notified20, boolean notified5, @NonNull List<TurnSignal> signals) {
            this.nextHintIdx = nextHintIdx;
            this.notified20 = notified20;
            this.notified5 = notified5;
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
            float speedMps,
            float accuracyMeters
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
                accuracyMeters
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
            float accuracyMeters
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
                accuracyMeters
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
            float accuracyMeters
    ) {
        if (hints.isEmpty() || nextHintIdx >= hints.size()) {
            return new Progress(nextHintIdx, notified20, notified5, Collections.emptyList());
        }

        List<TurnSignal> signals = new ArrayList<>();
        AdvanceCursor cursor = consumePassedHints(
                hints,
                hintAlongTrackMeters,
                nextHintIdx,
                notified20,
                notified5,
                alongTrackMeters,
                signals
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
                speedMps,
                signals
        );
    }

    @NonNull
    private AdvanceCursor consumePassedHints(
            @NonNull List<VoiceHint> hints,
            @NonNull List<Double> hintAlongTrackMeters,
            int nextHintIdx,
            boolean notified20,
            boolean notified5,
            double alongTrackMeters,
            @NonNull List<TurnSignal> signals
    ) {
        int updatedHintIdx = nextHintIdx;
        boolean updatedNotified20 = notified20;
        boolean updatedNotified5 = notified5;
        while (updatedHintIdx < hints.size()
                && hasPassedHint(hintAlongTrackMeters.get(updatedHintIdx), alongTrackMeters)) {
            signals.add(TurnSignal.passed(hints.get(updatedHintIdx)));
            updatedHintIdx++;
            updatedNotified20 = false;
            updatedNotified5 = false;
        }
        return new AdvanceCursor(updatedHintIdx, updatedNotified20, updatedNotified5);
    }

    private static boolean hasPassedHint(
            double hintAlongTrackMeters,
            double alongTrackMeters
    ) {
        return alongTrackMeters >= hintAlongTrackMeters + PASSED_HINT_BUFFER_METERS;
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
            float speedMps,
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
                speedMps
        );
        if (timeToNextSeconds == null) {
            return cursor.toProgress(signals);
        }
        if (!isImminentTurnDistanceReliable(distanceToNextMeters, speedMps, timeToNextSeconds)) {
            return cursor.toProgress(signals);
        }

        boolean updatedNotified20 = cursor.notified20;
        boolean updatedNotified5 = cursor.notified5;
        if (!cursor.notified5 && timeToNextSeconds <= VERY_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified20 = true;
            updatedNotified5 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
            return new Progress(cursor.nextHintIdx, updatedNotified20, updatedNotified5, signals);
        }
        if (!cursor.notified20 && timeToNextSeconds <= PREPARATORY_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified20 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
        }
        return new Progress(cursor.nextHintIdx, updatedNotified20, updatedNotified5, signals);
    }

    private static final class AdvanceCursor {
        public final int nextHintIdx;
        public final boolean notified20;
        public final boolean notified5;

        private AdvanceCursor(int nextHintIdx, boolean notified20, boolean notified5) {
            this.nextHintIdx = nextHintIdx;
            this.notified20 = notified20;
            this.notified5 = notified5;
        }

        @NonNull
        public Progress toProgress(@NonNull List<TurnSignal> signals) {
            return new Progress(nextHintIdx, notified20, notified5, signals);
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
        if (initialTurnNotificationSent || hints.isEmpty() || nextHintIdx < 0 || nextHintIdx >= hints.size()) {
            return null;
        }

        VoiceHint next = hints.get(nextHintIdx);
        double hintDistMeters = hintAlongTrackMeters.get(nextHintIdx);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        if (!isInitialTurnDistanceReliable(distanceToNextMeters, accuracyMeters)) {
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

    private boolean isInitialTurnDistanceReliable(double distanceToNextMeters, float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        double minTrustedDistanceMeters = Math.max(MIN_TRUSTED_TURN_DISTANCE_METERS, safeAccuracyMeters);
        return distanceToNextMeters > minTrustedDistanceMeters;
    }

    private boolean isImminentTurnDistanceReliable(
            double distanceToNextMeters,
            float speedMps,
            double timeToNextSeconds
    ) {
        return distanceToNextMeters > minimumTrustedImminentDistanceMeters(
                speedMps,
                distanceToNextMeters,
                timeToNextSeconds
        );
    }

    private static double minimumTrustedImminentDistanceMeters(
            float speedMps,
            double distanceToNextMeters,
            double timeToNextSeconds
    ) {
        double effectiveSpeedMps = resolveEffectiveSpeedMps(speedMps, distanceToNextMeters, timeToNextSeconds);
        if (effectiveSpeedMps <= 0.0) {
            return MIN_TRUSTED_TURN_DISTANCE_METERS;
        }
        double slowSpeedDistanceMeters = Math.max(
                MIN_SLOW_SPEED_TURN_DISTANCE_METERS,
                effectiveSpeedMps * MIN_ACTIONABLE_NOTICE_SECONDS
        );
        return Math.min(MIN_TRUSTED_TURN_DISTANCE_METERS, slowSpeedDistanceMeters);
    }

    private static double resolveEffectiveSpeedMps(
            float speedMps,
            double distanceToNextMeters,
            double timeToNextSeconds
    ) {
        if (Float.isFinite(speedMps) && speedMps > 0f) {
            return speedMps;
        }
        if (!Double.isFinite(timeToNextSeconds) || timeToNextSeconds <= 0.0) {
            return 0.0;
        }
        return distanceToNextMeters / timeToNextSeconds;
    }

}
