package vibro.navigator.nav.guidance;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class NavigationTurnState {
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();
    private final NavigationGuidanceHintSequence guidanceHints = new NavigationGuidanceHintSequence();

    private int nextHintIdx;
    private int routeHintCount;
    private boolean notified10;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    private boolean destinationReached;
    private int intermediateDestinationReachedTrackIndex = -1;

    public void reset() {
        guidanceHints.reset();
        nextHintIdx = 0;
        routeHintCount = 0;
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
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

    @NonNull
    public Progress evaluate(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            long nowMs,
            long fastChecksUntilMs
    ) {
        TurnEventPlanner.Progress progress = turnEventPlanner.advance(
                route,
                polylineIndex,
                guidanceHints.hints(),
                guidanceHints.hintAlongTrackMeters(),
                guidanceHints.nextIndex(),
                notified10,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
        guidanceHints.advanceTo(progress.nextHintIdx);
        syncNextRouteHintIndex(route);
        notified10 = progress.notified10;
        notified5 = progress.notified5;
        clearIntermediateDestinationReachedIfPassed(polylineIndex, alongTrackMeters);
        long suggestedUpdateIntervalMs = updateScheduler.suggestUpdateInterval(
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
        return new Progress(toTurnEvents(progress.signals), suggestedUpdateIntervalMs);
    }

    @NonNull
    public List<NavigationTurnEvent> onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        return onRouteApplied(route, polylineIndex, new ArrayList<>(), lastFiltered, speedMps, accuracyMeters);
    }

    @NonNull
    public List<NavigationTurnEvent> onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @NonNull List<LatLon> intermediateStops,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        routeHintCount = route.voiceHints.size();
        guidanceHints.onRouteApplied(route, polylineIndex, intermediateStops, lastFiltered);
        syncNextRouteHintIndex(route);
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        intermediateDestinationReachedTrackIndex = -1;
        return buildInitialTurnEventIfNeeded(route, polylineIndex, lastFiltered, speedMps, accuracyMeters);
    }

    @NonNull
    public List<NavigationTurnEvent> onDestinationReached(@NonNull GeoJsonRoute route) {
        if (destinationReached || route.track.isEmpty()) {
            return noTurnEvents();
        }
        nextHintIdx = route.voiceHints.size();
        routeHintCount = route.voiceHints.size();
        guidanceHints.advanceToEnd();
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = true;
        destinationReached = true;
        intermediateDestinationReachedTrackIndex = -1;
        VoiceHint arrivalHint = new VoiceHint(route.track.size() - 1, 100, 0, 0.0, 0);
        return oneTurnEvent(NavigationTurnEvent.imminent(arrivalHint, 0.0, 0.0));
    }

    @NonNull
    public List<NavigationTurnEvent> onIntermediateDestinationReached(int trackIndex) {
        initialTurnNotificationSent = true;
        notified10 = false;
        notified5 = false;
        intermediateDestinationReachedTrackIndex = trackIndex;
        guidanceHints.advancePastIntermediateDestination(trackIndex);
        syncNextRouteHintIndex();
        VoiceHint arrivalHint = new VoiceHint(trackIndex, 101, 0, 0.0, 0);
        return oneTurnEvent(NavigationTurnEvent.imminent(arrivalHint, 0.0, 0.0));
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
    private List<NavigationTurnEvent> buildInitialTurnEventIfNeeded(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        if (initialTurnNotificationSent) {
            return noTurnEvents();
        }
        if (guidanceHints.hints().isEmpty()
                || guidanceHints.nextIndex() < 0
                || guidanceHints.nextIndex() >= guidanceHints.hints().size()) {
            return noTurnEvents();
        }

        RoutePosition routePosition = resolveRoutePosition(polylineIndex, lastFiltered);

        TurnEventPlanner.TurnSignal initialSignal = turnEventPlanner.buildInitialSignal(
                route,
                polylineIndex,
                guidanceHints.hints(),
                guidanceHints.hintAlongTrackMeters(),
                guidanceHints.nextIndex(),
                initialTurnNotificationSent,
                routePosition.alongTrackMeters,
                routePosition.segmentIndex,
                speedMps,
                accuracyMeters
        );
        if (initialSignal == null) {
            return noTurnEvents();
        }
        initialTurnNotificationSent = true;
        return oneTurnEvent(toTurnEvent(initialSignal));
    }

    @NonNull
    private static RoutePosition resolveRoutePosition(
            @NonNull PolylineIndex polylineIndex,
            @Nullable Location lastFiltered
    ) {
        if (lastFiltered == null) {
            return RoutePosition.unknown();
        }
        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                -1
        );
        return match == null ? RoutePosition.unknown() : RoutePosition.from(match);
    }

    private static final class RoutePosition {
        public final double alongTrackMeters;
        public final int segmentIndex;

        private RoutePosition(double alongTrackMeters, int segmentIndex) {
            this.alongTrackMeters = alongTrackMeters;
            this.segmentIndex = segmentIndex;
        }

        @NonNull
        public static RoutePosition unknown() {
            return new RoutePosition(0.0, -1);
        }

        @NonNull
        public static RoutePosition from(@NonNull PolylineIndex.Match match) {
            return new RoutePosition(match.alongTrackMeters, match.segmentIndex);
        }
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
