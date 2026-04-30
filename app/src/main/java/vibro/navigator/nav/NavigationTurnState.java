package vibro.navigator.nav;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationTurnState {

    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();

    private int nextHintIdx;
    private boolean notified10;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    private boolean destinationReached;

    void reset() {
        nextHintIdx = 0;
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
    }

    int getNextHintIdx() {
        return nextHintIdx;
    }

    boolean isDestinationReached() {
        return destinationReached;
    }

    @NonNull
    Progress evaluate(
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
                nextHintIdx,
                notified10,
                notified5,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps,
                accuracyMeters
        );
        nextHintIdx = progress.nextHintIdx;
        notified10 = progress.notified10;
        notified5 = progress.notified5;
        long suggestedUpdateIntervalMs = updateScheduler.suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route,
                polylineIndex,
                nextHintIdx,
                alongTrackMeters,
                currentSegmentIndex,
                speedMps
        );
        return new Progress(toTurnEvents(progress.signals), suggestedUpdateIntervalMs);
    }

    @NonNull
    List<NavigationTurnEvent> onRouteApplied(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters
    ) {
        nextHintIdx = findNextHintIndex(route, polylineIndex, lastFiltered);
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        destinationReached = false;
        return buildInitialTurnEventIfNeeded(route, polylineIndex, lastFiltered, speedMps, accuracyMeters);
    }

    @NonNull
    List<NavigationTurnEvent> onDestinationReached(@NonNull GeoJsonRoute route) {
        if (destinationReached || route.track.isEmpty()) {
            return Collections.emptyList();
        }
        nextHintIdx = route.voiceHints.size();
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = true;
        destinationReached = true;
        VoiceHint arrivalHint = new VoiceHint(route.track.size() - 1, 100, 0, 0.0, 0);
        return Collections.singletonList(NavigationTurnEvent.imminent(arrivalHint, 0.0, 0.0));
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
            return Collections.emptyList();
        }
        List<VoiceHint> hints = route.voiceHints;
        if (hints.isEmpty() || nextHintIdx < 0 || nextHintIdx >= hints.size()) {
            return Collections.emptyList();
        }

        RoutePosition routePosition = resolveRoutePosition(polylineIndex, lastFiltered);

        TurnEventPlanner.TurnSignal initialSignal = turnEventPlanner.buildInitialSignal(
                route,
                polylineIndex,
                nextHintIdx,
                initialTurnNotificationSent,
                routePosition.alongTrackMeters,
                routePosition.segmentIndex,
                speedMps,
                accuracyMeters
        );
        if (initialSignal == null) {
            return Collections.emptyList();
        }
        initialTurnNotificationSent = true;
        return Collections.singletonList(toTurnEvent(initialSignal));
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
        final double alongTrackMeters;
        final int segmentIndex;

        private RoutePosition(double alongTrackMeters, int segmentIndex) {
            this.alongTrackMeters = alongTrackMeters;
            this.segmentIndex = segmentIndex;
        }

        @NonNull
        static RoutePosition unknown() {
            return new RoutePosition(0.0, -1);
        }

        @NonNull
        static RoutePosition from(@NonNull PolylineIndex.Match match) {
            return new RoutePosition(match.alongTrackMeters, match.segmentIndex);
        }
    }

    private int findNextHintIndex(
            @NonNull GeoJsonRoute candidateRoute,
            @NonNull PolylineIndex candidateIndex,
            @Nullable Location location
    ) {
        if (location == null || candidateRoute.voiceHints.isEmpty()) {
            return 0;
        }

        PolylineIndex.Match match = candidateIndex.match(
                new LatLon(location.getLatitude(), location.getLongitude()),
                -1
        );
        if (match == null) {
            return 0;
        }

        for (int i = 0; i < candidateRoute.voiceHints.size(); i++) {
            VoiceHint hint = candidateRoute.voiceHints.get(i);
            double hintDistance = candidateIndex.distanceAtPointIndex(hint.indexInTrack);
            if (hintDistance + 5.0 > match.alongTrackMeters) {
                return i;
            }
        }
        return candidateRoute.voiceHints.size();
    }

    @NonNull
    private List<NavigationTurnEvent> toTurnEvents(@NonNull List<TurnEventPlanner.TurnSignal> signals) {
        if (signals.isEmpty()) {
            return Collections.emptyList();
        }
        List<NavigationTurnEvent> events = new ArrayList<>(signals.size());
        for (TurnEventPlanner.TurnSignal signal : signals) {
            events.add(toTurnEvent(signal));
        }
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

    static final class Progress {
        @NonNull
        final List<NavigationTurnEvent> turnEvents;
        final long suggestedUpdateIntervalMs;

        private Progress(@NonNull List<NavigationTurnEvent> turnEvents, long suggestedUpdateIntervalMs) {
            this.turnEvents = turnEvents;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
        }
    }
}
