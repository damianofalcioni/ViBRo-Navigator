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
        int updatedHintIdx = nextHintIdx;
        boolean updatedNotified10 = notified10;
        boolean updatedNotified5 = notified5;

        while (updatedHintIdx < hints.size()) {
            VoiceHint next = hints.get(updatedHintIdx);
            double hintDistMeters = polylineIndex.distanceAtPointIndex(next.indexInTrack);
            if (alongTrackMeters >= hintDistMeters + PASSED_HINT_BUFFER_METERS) {
                signals.add(TurnSignal.passed(next));
                updatedHintIdx++;
                updatedNotified10 = false;
                updatedNotified5 = false;
                continue;
            }
            break;
        }

        if (updatedHintIdx >= hints.size()) {
            return new Progress(updatedHintIdx, updatedNotified10, updatedNotified5, signals);
        }

        VoiceHint next = hints.get(updatedHintIdx);
        double hintDistMeters = polylineIndex.distanceAtPointIndex(next.indexInTrack);
        double distanceToNextMeters = Math.max(0.0, hintDistMeters - alongTrackMeters);
        if (!isImminentTurnDistanceReliable(distanceToNextMeters)) {
            return new Progress(updatedHintIdx, updatedNotified10, updatedNotified5, signals);
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
            return new Progress(updatedHintIdx, updatedNotified10, updatedNotified5, signals);
        }

        if (!updatedNotified5 && timeToNextSeconds <= VERY_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified5 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
            return new Progress(updatedHintIdx, updatedNotified10, updatedNotified5, signals);
        }
        if (!updatedNotified10 && timeToNextSeconds <= INITIAL_IMMINENT_THRESHOLD_SECONDS) {
            updatedNotified10 = true;
            signals.add(TurnSignal.imminent(next, distanceToNextMeters, timeToNextSeconds));
        }
        return new Progress(updatedHintIdx, updatedNotified10, updatedNotified5, signals);
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
