package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

public final class NavigationTurnState {
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();
    private final NavigationGuidanceHintSequence guidanceHints = new NavigationGuidanceHintSequence();
    private final NavigationTurnManeuverCueState maneuverCueState = new NavigationTurnManeuverCueState();

    private int nextHintIdx;
    private int routeHintCount;
    private boolean notified20;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    private boolean destinationReached;
    private int intermediateDestinationReachedTrackIndex = -1;

    public void reset() {
        guidanceHints.reset();
        nextHintIdx = 0;
        routeHintCount = 0;
        notified20 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
        updateScheduler.resetPostManeuverIntervalRamp();
        maneuverCueState.clear();
    }

    public int getNextHintIdx() {
        return nextHintIdx;
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
                singleInstructionMode
        );
        guidanceHints.advanceTo(progress.nextHintIdx);
        syncNextRouteHintIndex(route);
        notified20 = progress.notified20;
        notified5 = progress.notified5;
        maneuverCueState.update(progress.signals);
        maneuverCueState.clearIfPassed(polylineIndex, alongTrackMeters);
        clearIntermediateDestinationReachedIfPassed(polylineIndex, alongTrackMeters);
        long naturalUpdateIntervalMs = updateScheduler.suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route,
                polylineIndex,
                guidanceHints.nextHint(),
                guidanceHints.nextAlongTrackMeters(),
                alongTrackMeters,
                currentSegmentIndex,
                speedMps
        );
        long suggestedUpdateIntervalMs = updateScheduler.applyPostManeuverIntervalRamp(
                naturalUpdateIntervalMs,
                progress.advancedPastInstruction,
                nowMs
        );
        return new Progress(toTurnEvents(progress.signals), suggestedUpdateIntervalMs);
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
        guidanceHints.onRouteApplied(route, polylineIndex, intermediateStops, lastFiltered);
        syncNextRouteHintIndex(route);
        notified20 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
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
        return oneTurnEvent(toTurnEvent(initialSignal));
    }

    private boolean hasPendingInitialTurnSignal() {
        return !guidanceHints.hints().isEmpty()
                && guidanceHints.nextIndex() >= 0
                && guidanceHints.nextIndex() < guidanceHints.hints().size();
    }

    private void syncNextRouteHintIndex(@NonNull GeoJsonRoute route) {
        nextHintIdx = guidanceHints.nextRouteHintIndex(route.voiceHints.size());
    }

    private void syncNextRouteHintIndex() {
        nextHintIdx = guidanceHints.nextRouteHintIndex(routeHintCount);
    }

    @NonNull
    private List<NavigationTurnEvent> toTurnEvents(@NonNull List<TurnEventPlanner.TurnSignal> signals) {
        if (signals.isEmpty()) {
            return noTurnEvents();
        }
        List<NavigationTurnEvent> events = new ArrayList<>(signals.size());
        for (TurnEventPlanner.TurnSignal signal : signals) {
            events.add(toTurnEvent(signal));
        }
        return events;
    }

    @NonNull
    private static List<NavigationTurnEvent> noTurnEvents() {
        return new ArrayList<>();
    }

    @NonNull
    private static List<NavigationTurnEvent> oneTurnEvent(@NonNull NavigationTurnEvent event) {
        List<NavigationTurnEvent> events = new ArrayList<>(1);
        events.add(event);
        return events;
    }

    @NonNull
    private NavigationTurnEvent toTurnEvent(@NonNull TurnEventPlanner.TurnSignal signal) {
        switch (signal.type) {
            case PASSED:
                return NavigationTurnEvent.passed(signal.hint);
            case INITIAL:
                return NavigationTurnEvent.initial(signal.hint, signal.distanceMeters, signal.timeSeconds);
            case IMMINENT:
            default:
                return NavigationTurnEvent.imminent(signal.hint, signal.distanceMeters, signal.timeSeconds);
        }
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
