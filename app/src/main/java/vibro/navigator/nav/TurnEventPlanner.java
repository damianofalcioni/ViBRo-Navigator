package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.route.GeoJsonRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TurnEventPlanner {

    private static final double PASSED_HINT_BUFFER_METERS = 5.0;
    private static final double INITIAL_IMMINENT_THRESHOLD_SECONDS = 10.0;
    private static final double VERY_IMMINENT_THRESHOLD_SECONDS = 5.0;
    private static final double MIN_TRUSTED_TURN_DISTANCE_METERS = 5.0;

    static final class Progress {
        final int nextHintIdx;
        final boolean notified10;
        final boolean notified5;
        @NonNull
        final List<TurnSignal> signals;

        private Progress(int nextHintIdx, boolean notified10, boolean notified5, @NonNull List<TurnSignal> signals) {
            this.nextHintIdx = nextHintIdx;
            this.notified10 = notified10;
            this.notified5 = notified5;
            this.signals = signals;
        }
    }

    static final class TurnSignal {
        enum Type {
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
        static TurnSignal passed(@NonNull VoiceHint hint) {
            return new TurnSignal(Type.PASSED, hint, 0.0, 0.0);
        }

        @NonNull
        static TurnSignal imminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnSignal(Type.IMMINENT, hint, distanceMeters, timeSeconds);
        }

        @NonNull
        static TurnSignal initial(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnSignal(Type.INITIAL, hint, distanceMeters, timeSeconds);
        }
    }

    @NonNull
    Progress advance(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean notified10,
            boolean notified5,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        List<VoiceHint> hints = route.voiceHints;
        if (hints.isEmpty() || nextHintIdx >= hints.size()) {
            return new Progress(nextHintIdx, notified10, notified5, Collections.emptyList());
        }

        List<TurnSignal> signals = new ArrayList<>();
        AdvanceCursor cursor = consumePassedHints(hints, polylineIndex, nextHintIdx, notified10, notified5, alongTrackMeters, signals);

        if (cursor.nextHintIdx >= hints.size()) {
            return cursor.toProgress(signals);
        }

        return appendImminentSignalIfNeeded(
                route,
                polylineIndex,
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
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean notified10,
            boolean notified5,
            double alongTrackMeters,
            @NonNull List<TurnSignal> signals
    ) {
        int updatedHintIdx = nextHintIdx;
        boolean updatedNotified10 = notified10;
        boolean updatedNotified5 = notified5;
        while (updatedHintIdx < hints.size() && hasPassedHint(polylineIndex, hints.get(updatedHintIdx), alongTrackMeters)) {
            signals.add(TurnSignal.passed(hints.get(updatedHintIdx)));
            updatedHintIdx++;
            updatedNotified10 = false;
            updatedNotified5 = false;
        }
        return new AdvanceCursor(updatedHintIdx, updatedNotified10, updatedNotified5);
    }

    private static boolean hasPassedHint(
            @NonNull PolylineIndex polylineIndex,
            @NonNull VoiceHint hint,
            double alongTrackMeters
    ) {
        return alongTrackMeters >= polylineIndex.distanceAtPointIndex(hint.indexInTrack) + PASSED_HINT_BUFFER_METERS;
    }

    @NonNull
    private Progress appendImminentSignalIfNeeded(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull AdvanceCursor cursor,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            @NonNull List<TurnSignal> signals
    ) {
        VoiceHint next = route.voiceHints.get(cursor.nextHintIdx);
        double hintDistMeters = polylineIndex.distanceAtPointIndex(next.indexInTrack);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        if (!isImminentTurnDistanceReliable(distanceToNextMeters)) {
            return cursor.toProgress(signals);
        }
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                next.indexInTrack,
                speedMps
        );
        if (timeToNextSeconds == null) {
            return cursor.toProgress(signals);
        }

        boolean updatedNotified10 = cursor.notified10;
        boolean updatedNotified5 = cursor.notified5;
        if (!cursor.notified5 && timeToNextSeconds <= VERY_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified5 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
            return new Progress(cursor.nextHintIdx, updatedNotified10, updatedNotified5, signals);
        }
        if (!cursor.notified10 && timeToNextSeconds <= INITIAL_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified10 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
        }
        return new Progress(cursor.nextHintIdx, updatedNotified10, updatedNotified5, signals);
    }

    private static final class AdvanceCursor {
        final int nextHintIdx;
        final boolean notified10;
        final boolean notified5;

        private AdvanceCursor(int nextHintIdx, boolean notified10, boolean notified5) {
            this.nextHintIdx = nextHintIdx;
            this.notified10 = notified10;
            this.notified5 = notified5;
        }

        @NonNull
        Progress toProgress(@NonNull List<TurnSignal> signals) {
            return new Progress(nextHintIdx, notified10, notified5, signals);
        }
    }

    @Nullable
    TurnSignal buildInitialSignal(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            int nextHintIdx,
            boolean initialTurnNotificationSent,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters
    ) {
        List<VoiceHint> hints = route.voiceHints;
        if (initialTurnNotificationSent || hints.isEmpty() || nextHintIdx < 0 || nextHintIdx >= hints.size()) {
            return null;
        }

        VoiceHint next = hints.get(nextHintIdx);
        double hintDistMeters = polylineIndex.distanceAtPointIndex(next.indexInTrack);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        if (!isInitialTurnDistanceReliable(distanceToNextMeters, accuracyMeters)) {
            return null;
        }
        Double timeToNextSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                next.indexInTrack,
                speedMps
        );
        return TurnSignal.initial(
                next,
                distanceToNextMeters,
                timeToNextSeconds != null ? timeToNextSeconds : Double.NaN
        );
    }

    private boolean isInitialTurnDistanceReliable(double distanceToNextMeters, float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        double minTrustedDistanceMeters = Math.max(MIN_TRUSTED_TURN_DISTANCE_METERS, safeAccuracyMeters);
        return distanceToNextMeters > minTrustedDistanceMeters;
    }

    private boolean isImminentTurnDistanceReliable(double distanceToNextMeters) {
        return distanceToNextMeters > MIN_TRUSTED_TURN_DISTANCE_METERS;
    }

}
