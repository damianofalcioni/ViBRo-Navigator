package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.location.NavigationLocation;

import java.util.List;

public final class NavigationTurnState {
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();
    private final NavigationGuidanceHintSequence guidanceHints = new NavigationGuidanceHintSequence();
    private final NavigationTurnManeuverCueState maneuverCueState = new NavigationTurnManeuverCueState();
    private final NavigationGuidanceDiagnostics diagnostics = new NavigationGuidanceDiagnostics();

    private int nextHintIdx;
    private int routeHintCount;
    private boolean notified20;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    private boolean destinationReached;
    private int intermediateDestinationReachedTrackIndex = -1;
    private boolean startupIntervalRampPending;

    public void reset() {
        diagnostics.reset();
        guidanceHints.reset();
        nextHintIdx = 0;
        routeHintCount = 0;
        notified20 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
        startupIntervalRampPending = false;
        updateScheduler.resetPostManeuverIntervalRamp();
        maneuverCueState.clear();
    }

    public int getNextHintIdx() {
        return nextHintIdx;
    }

    public void recordLocationQuality(NavigationLocation location, float accuracy) {
        updateScheduler.recordLocationQuality(location, accuracy);
    }

    public boolean isDestinationReached() {
        return destinationReached;
    }

    public int getIntermediateDestinationReachedTrackIndex() {
        return intermediateDestinationReachedTrackIndex;
    }

    @Nullable
    public Integer getActiveTurnManeuverDegrees() {
        return maneuverCueState.activeTurnManeuverDegrees();
    }

    @Nullable
    public Integer getActiveTurnManeuverTrackIndex() {
        return maneuverCueState.activeTurnManeuverTrackIndex();
    }

    @Nullable
    public Integer getNextTurnManeuverDegrees() {
        return guidanceHints.nextTurnManeuverDegrees();
    }

    @Nullable
    public Integer getNextTurnManeuverTrackIndex() {
        return guidanceHints.nextTurnManeuverTrackIndex();
    }

    @NonNull
    public Progress evaluate(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            long fastChecksUntilMs
    ) {
        return evaluate(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                nowMs,
                fastChecksUntilMs,
                false
        );
    }

    @NonNull
    public Progress evaluate(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
        return evaluate(
                route,
                polylineIndex,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                Float.NaN,
                true,
                nowMs,
                fastChecksUntilMs,
                singleInstructionMode
        );
    }

    @NonNull
    public Progress evaluate(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accelerationMps2,
            boolean trustworthyProgress,
            long nowMs,
            long fastChecksUntilMs,
            boolean singleInstructionMode
    ) {
        TurnEventPlanner.Progress progress = turnEventPlanner.advance(
                route,
                polylineIndex,
                guidanceHints.hints(),
                guidanceHints.hintAlongTrackMeters(),
                guidanceHints.nextIndex(),
                notified20,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accelerationMps2,
                trustworthyProgress,
                TurnNotificationPlan.from(singleInstructionMode)
        );
        guidanceHints.advanceTo(progress.nextHintIdx);
        syncNextRouteHintIndex(route);
        notified20 = progress.notified20;
        notified5 = progress.notified5;
        maneuverCueState.update(progress.signals);
        if (trustworthyProgress) {
            maneuverCueState.clearIfPassed(polylineIndex, alongTrackMeters);
            clearIntermediateDestinationReachedIfPassed(polylineIndex, alongTrackMeters);
        }
        long naturalUpdateIntervalMs = updateScheduler.suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route,
                polylineIndex,
                guidanceHints.nextHint(),
                guidanceHints.nextAlongTrackMeters(),
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accelerationMps2
        );
        boolean startupIntervalRampTriggered = startupIntervalRampPending
                & fastChecksUntilMs > 0L
                & nowMs > fastChecksUntilMs;
        startupIntervalRampPending = startupIntervalRampPending
                & fastChecksUntilMs > 0L
                & !startupIntervalRampTriggered
                & !progress.advancedPastInstruction;
        boolean restartIntervalRamp = progress.advancedPastInstruction | startupIntervalRampTriggered;
        long suggestedUpdateIntervalMs = updateScheduler.applyPostManeuverIntervalRamp(
                naturalUpdateIntervalMs,
                restartIntervalRamp,
                nowMs
        );
        diagnostics.record(progress.decision, suggestedUpdateIntervalMs, updateScheduler.intervalReason(),
                trustworthyProgress, progress.signals.size(), nowMs);
        return new Progress(NavigationTurnSignalEvents.convert(progress.signals), suggestedUpdateIntervalMs);
    }

    @NonNull
    public List<NavigationTurnEvent> onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops,
            @Nullable LatLon lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        routeHintCount = route.voiceHints.size();
        diagnostics.reset();
        guidanceHints.onRouteApplied(route, polylineIndex, intermediateStops, lastFiltered);
        syncNextRouteHintIndex(route);
        notified20 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
        startupIntervalRampPending = true;
        updateScheduler.resetPostManeuverIntervalRamp();
        maneuverCueState.clear();
        return buildInitialTurnEventIfNeeded(
                route,
                polylineIndex,
                lastFiltered,
                speedMps,
                accuracyMeters
        );
    }

    @NonNull
    public List<NavigationTurnEvent> onDestinationReached(@NonNull GeoJsonRoute route) {
        if (destinationReached || route.track.isEmpty()) {
            return noTurnEvents();
        }
        nextHintIdx = route.voiceHints.size();
        routeHintCount = route.voiceHints.size();
        guidanceHints.advanceToEnd();
        notified20 = false;
        notified5 = false;
        initialTurnNotificationSent = true;
        destinationReached = true;
        intermediateDestinationReachedTrackIndex = -1;
        startupIntervalRampPending = false;
        updateScheduler.resetPostManeuverIntervalRamp();
        maneuverCueState.clear();
        return NavigationTurnManeuverCueState.destinationArrival(route.track.size() - 1);
    }

    @NonNull
    public List<NavigationTurnEvent> onIntermediateDestinationReached(int trackIndex) {
        initialTurnNotificationSent = true;
        notified20 = false;
        notified5 = false;
        intermediateDestinationReachedTrackIndex = trackIndex;
        startupIntervalRampPending = false;
        updateScheduler.resetPostManeuverIntervalRamp();
        maneuverCueState.clear();
        guidanceHints.advancePastIntermediateDestination(trackIndex);
        syncNextRouteHintIndex();
        return NavigationTurnManeuverCueState.intermediateArrival(trackIndex);
    }

    private void clearIntermediateDestinationReachedIfPassed(
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters
    ) {
        if (intermediateDestinationReachedTrackIndex < 0) {
            return;
        }
        double reachedDistanceMeters = polylineIndex.distanceAtPointIndex(intermediateDestinationReachedTrackIndex);
        if (alongTrackMeters > reachedDistanceMeters + 5.0) {
            intermediateDestinationReachedTrackIndex = -1;
        }
    }

    @NonNull
    public List<NavigationTurnEvent> buildInitialTurnEventIfNeeded(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable LatLon lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        if (initialTurnNotificationSent) {
            return noTurnEvents();
        }
        if (!hasPendingInitialTurnSignal()) {
            return noTurnEvents();
        }

        PolylineIndex.Match match = lastFiltered == null ? null : polylineIndex.match(lastFiltered, -1);
        double alongTrackMeters = match == null ? 0.0 : match.alongTrackMeters;
        int segmentIndex = match == null ? -1 : match.segmentIndex;

        TurnEventPlanner.TurnSignal initialSignal = turnEventPlanner.buildInitialSignal(
                route,
                polylineIndex,
                guidanceHints.hints(),
                guidanceHints.hintAlongTrackMeters(),
                guidanceHints.nextIndex(),
                initialTurnNotificationSent,
                alongTrackMeters,
                segmentIndex,
                speedMps,
                accuracyMeters
        );
        if (initialSignal == null) {
            return noTurnEvents();
        }
        initialTurnNotificationSent = true;
        return NavigationTurnSignalEvents.single(initialSignal);
    }

    private boolean hasPendingInitialTurnSignal() {
        return !guidanceHints.hints().isEmpty()
                && guidanceHints.nextIndex() >= 0
                && guidanceHints.nextIndex() < guidanceHints.hints().size()
                && !NavigationTurnManeuverCueState.isSyntheticArrivalHint(guidanceHints.nextHint());
    }

    private void syncNextRouteHintIndex(@NonNull GeoJsonRoute route) {
        nextHintIdx = guidanceHints.nextRouteHintIndex(route.voiceHints.size());
    }

    private void syncNextRouteHintIndex() {
        nextHintIdx = guidanceHints.nextRouteHintIndex(routeHintCount);
    }

    @NonNull
    private static List<NavigationTurnEvent> noTurnEvents() {
        return NavigationTurnSignalEvents.empty();
    }

    public static final class Progress {
        @NonNull
        public final List<NavigationTurnEvent> turnEvents;
        public final long suggestedUpdateIntervalMs;

        private Progress(@NonNull List<NavigationTurnEvent> turnEvents, long suggestedUpdateIntervalMs) {
            this.turnEvents = turnEvents;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
        }
    }
}
