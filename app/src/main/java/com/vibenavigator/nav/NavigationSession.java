package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.Collections;
import java.util.List;

final class NavigationSession {

    private static final String TAG = "NavigationSession";
    private static final long NO_SUGGESTED_INTERVAL = -1L;

    private final NavigationSessionLocationState locationState = new NavigationSessionLocationState();
    private final NavigationSessionRouteState routeState = new NavigationSessionRouteState();
    private final NavigationWarmupController warmupController = new NavigationWarmupController();
    private final NavigationRouteRequestManager routeRequestManager = new NavigationRouteRequestManager();

    @NonNull
    private NavigationRequest currentRequest = new NavigationRequest(null, null, null, Collections.emptyList());

    void loadRequest(@NonNull NavigationRequest request) {
        currentRequest = request;
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    boolean start(@NonNull Context context, long nowMs) {
        locationState.reset();
        routeState.reset();
        warmupController.reset(nowMs);
        routeRequestManager.reset(nowMs);

        if (!currentRequest.isComplete()) {
            routeRequestManager.markInvalidRequest(context);
            AppLogger.e(TAG, "Navigation start aborted because the request is incomplete "
                    + currentRequest.describe(), null);
            return false;
        }
        return true;
    }

    void stop() {
        routeRequestManager.stop();
    }

    boolean hasActiveRoute() {
        return routeState.hasActiveRoute();
    }

    void onProviderDisabled(@NonNull String provider) {
        locationState.onProviderDisabled(provider);
    }

    @Nullable
    Location getLastFilteredLocation() {
        return locationState.getLastFilteredLocation();
    }

    float lastFilteredSpeedMps() {
        Location lastFiltered = locationState.getLastFilteredLocation();
        return lastFiltered == null ? 0f : locationState.speedMps(lastFiltered);
    }

    boolean isLikelyStationaryForOrientation() {
        return locationState.isLikelyStationary();
    }

    boolean isRouteCalculationInProgress() {
        return routeRequestManager.isRouteCalculationInProgress();
    }

    @Nullable
    Double currentRouteBearingDegrees() {
        return routeState.currentSegmentBearingDegrees(locationState.getLastFilteredLocation());
    }

    @NonNull
    NavigationRequest currentNavigationRequest() {
        return currentRequest;
    }

    @NonNull
    LocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull Location location, long nowMs) {
        NavigationSessionLocationState.Update update = locationState.onRawLocationChanged(location);
        if (update.isDropped()) {
            return LocationUpdateResult.dropped();
        }

        Location filtered = update.getFilteredLocation();
        routeRequestManager.clearRouteFailure();
        if (!currentRequest.isComplete()) {
            routeRequestManager.markInvalidRequest(context);
            AppLogger.e(TAG, "Skipping route evaluation because the request is incomplete "
                    + currentRequest.describe(), null);
            return LocationUpdateResult.accepted(filtered, true, null, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }

        NavigationSessionRouteState.Evaluation evaluation = routeState.evaluateLocation(
                filtered,
                locationState.speedMps(filtered),
                locationState.accuracyMeters(filtered),
                locationState.actualBearingDegrees(filtered),
                nowMs,
                warmupController.getFastChecksUntilMs()
        );
        warmupController.recordEvaluation(evaluation.isStableOnRouteSample(), locationState.accuracyMeters(filtered), nowMs);
        return LocationUpdateResult.accepted(
                filtered,
                evaluation.shouldRecalculateRoute(),
                evaluation.rerouteNotice,
                evaluation.turnEvents,
                evaluation.getSuggestedUpdateIntervalMs()
        );
    }

    @NonNull
    List<NogoPoint> addBlockedPointsAhead() {
        return routeState.addBlockedPointsAhead(locationState.getLastFilteredLocation(), System.currentTimeMillis());
    }

    @Nullable
    RouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs) {
        return routeRequestManager.prepare(
                force,
                nowMs,
                currentRequest,
                locationState.getLastFilteredLocation(),
                routeState.copyBlockedPoints()
        );
    }

    @NonNull
    List<TurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull RouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (!routeRequestManager.onRouteApplied(snapshot)) {
            return Collections.emptyList();
        }
        warmupController.onRouteApplied();
        Location lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        return routeState.applyRouteResult(context, currentRequest, snapshot, newRoute, lastFiltered, speedMps, beganAt);
    }

    void applyRouteFailure(
            @NonNull Context context,
            @NonNull RouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        routeRequestManager.onRouteFailure(context, snapshot, error);
    }

    boolean consumePendingRouteRecalculation() {
        return routeRequestManager.consumePendingRecalculation();
    }

    @NonNull
    NavState buildState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees
    ) {
        Location lastFiltered = locationState.getLastFilteredLocation();
        float speedMps = lastFiltered != null ? locationState.speedMps(lastFiltered) : 0f;
        float accuracyMeters = lastFiltered != null
                ? locationState.accuracyMeters(lastFiltered)
                : Float.MAX_VALUE;
        Double headingDegrees = displayHeadingDegrees;
        Float headingAccuracyDegrees = displayHeadingAccuracyDegrees;
        if (headingDegrees == null) {
            headingDegrees = lastFiltered != null
                    ? locationState.actualBearingDegrees(lastFiltered)
                    : null;
            headingAccuracyDegrees = null;
        }
        return routeState.buildState(
                context,
                lastFiltered,
                speedMps,
                locationState.isLikelyStationary(),
                accuracyMeters,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                routeRequestManager.isRouteCalculationInProgress(),
                routeRequestManager.getLastRouteFailure()
        );
    }

    static final class LocationUpdateResult {
        private final boolean dropped;
        private final boolean shouldRecalculateRoute;
        private final long suggestedUpdateIntervalMs;
        @Nullable
        private final NavigationRerouteNotice rerouteNotice;
        @Nullable
        final Location filteredLocation;
        @NonNull
        final List<TurnEvent> turnEvents;

        private LocationUpdateResult(
                boolean dropped,
                boolean shouldRecalculateRoute,
                long suggestedUpdateIntervalMs,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @Nullable Location filteredLocation,
                @NonNull List<TurnEvent> turnEvents
        ) {
            this.dropped = dropped;
            this.shouldRecalculateRoute = shouldRecalculateRoute;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
            this.rerouteNotice = rerouteNotice;
            this.filteredLocation = filteredLocation;
            this.turnEvents = turnEvents;
        }

        @NonNull
        static LocationUpdateResult dropped() {
            return new LocationUpdateResult(true, false, NO_SUGGESTED_INTERVAL, null, null, Collections.emptyList());
        }

        @NonNull
        static LocationUpdateResult accepted(
                @Nullable Location filteredLocation,
                boolean shouldRecalculateRoute,
                @Nullable NavigationRerouteNotice rerouteNotice,
                @NonNull List<TurnEvent> turnEvents,
                long suggestedUpdateIntervalMs
        ) {
            return new LocationUpdateResult(
                    false,
                    shouldRecalculateRoute,
                    suggestedUpdateIntervalMs,
                    rerouteNotice,
                    filteredLocation,
                    turnEvents
            );
        }

        boolean isDropped() {
            return dropped;
        }

        boolean shouldRecalculateRoute() {
            return shouldRecalculateRoute;
        }

        long getSuggestedUpdateIntervalMs() {
            return suggestedUpdateIntervalMs;
        }

        @Nullable
        NavigationRerouteNotice getRerouteNotice() {
            return rerouteNotice;
        }
    }

    static final class TurnEvent {
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

        private TurnEvent(@NonNull Type type, @NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.type = type;
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }

        @NonNull
        static TurnEvent passed(@NonNull VoiceHint hint) {
            return new TurnEvent(Type.PASSED, hint, 0.0, 0.0);
        }

        @NonNull
        static TurnEvent imminent(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnEvent(Type.IMMINENT, hint, distanceMeters, timeSeconds);
        }

        @NonNull
        static TurnEvent initial(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            return new TurnEvent(Type.INITIAL, hint, distanceMeters, timeSeconds);
        }
    }

    static final class RouteRequestSnapshot {
        final int requestNumber;
        final int requestToken;
        @NonNull
        final LatLon start;
        @NonNull
        final List<LatLon> intermediates;
        @Nullable
        final LatLon destination;
        @Nullable
        final String profile;
        @NonNull
        final List<NogoPoint> blocked;

        RouteRequestSnapshot(
                int requestNumber,
                int requestToken,
                @NonNull LatLon start,
                @NonNull List<LatLon> intermediates,
                @Nullable LatLon destination,
                @Nullable String profile,
                @NonNull List<NogoPoint> blocked
        ) {
            this.requestNumber = requestNumber;
            this.requestToken = requestToken;
            this.start = start;
            this.intermediates = intermediates;
            this.destination = destination;
            this.profile = profile;
            this.blocked = blocked;
        }
    }
}
