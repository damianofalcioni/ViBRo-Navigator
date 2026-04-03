package com.vibenavigator.nav;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;
import com.vibenavigator.brouter.NogoPoint;
import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.kalman.LatLonKalmanFilter;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;
import com.vibenavigator.util.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NavigationSession {

    private static final String TAG = "NavigationSession";
    private static final double BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS = 20.0;
    private static final double BLOCKED_ROUTE_POINT_STEP_METERS = 18.0;
    private static final double BLOCKED_RADIUS_BASE_METERS = 12.0;
    private static final double BLOCKED_RADIUS_STEP_METERS = 6.0;
    private static final double BLOCKED_RADIUS_MAX_METERS = 30.0;
    private static final int BLOCKED_POINT_COUNT_MAX = 3;
    private static final double BLOCKED_SAME_AREA_METERS = 35.0;
    private static final double BLOCKED_QUICK_REPEAT_NEARBY_METERS = 75.0;
    private static final long BLOCKED_QUICK_REPEAT_WINDOW_MS = 15_000L;

    private final LatLonKalmanFilter kalman = new LatLonKalmanFilter();
    private final LiveLocationCoordinator liveLocationCoordinator = new LiveLocationCoordinator();
    private final RouteDeviationPolicy routeDeviationPolicy = new RouteDeviationPolicy();
    private final NavigationUpdateScheduler updateScheduler = new NavigationUpdateScheduler();
    private final TurnEventPlanner turnEventPlanner = new TurnEventPlanner();

    @Nullable
    private String profile;
    @Nullable
    private String destinationName;
    @Nullable
    private LatLon destination;
    @NonNull
    private List<LatLon> intermediates = new ArrayList<>();
    @NonNull
    private final List<NogoPoint> blocked = new ArrayList<>();

    @Nullable
    private Location lastFiltered;
    @Nullable
    private Location prevFiltered;

    @Nullable
    private GeoJsonRoute route;
    @Nullable
    private PolylineIndex polylineIndex;
    private int lastSegmentIndex = -1;
    private int nextHintIdx;
    private boolean notified10;
    private boolean notified5;
    private boolean initialTurnNotificationSent;
    private long fastChecksUntilMs;
    private long lastRerouteMs;
    @NonNull
    private List<NavTarget> targets = new ArrayList<>();
    private int locationUpdateCount;
    private int routeRequestCount;
    private int routeRequestToken;
    private boolean routeCalculationInProgress;
    @Nullable
    private String lastRouteFailureMessage;
    @Nullable
    private LatLon lastBlockedAreaCenter;
    private long lastBlockedAreaAtMs;
    private int lastBlockedAreaLevel;

    void loadRequest(@NonNull NavigationRequest request) {
        profile = request.profile;
        destinationName = request.destinationName;
        destination = request.destination;
        intermediates = new ArrayList<>(request.stops);
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    boolean start(@NonNull Context context, long nowMs) {
        invalidatePendingRouteResults();
        blocked.clear();
        route = null;
        polylineIndex = null;
        lastSegmentIndex = -1;
        nextHintIdx = 0;
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        targets = new ArrayList<>();
        fastChecksUntilMs = nowMs + 30_000L;
        lastRerouteMs = 0L;
        locationUpdateCount = 0;
        routeRequestCount = 0;
        routeCalculationInProgress = false;
        lastRouteFailureMessage = null;
        lastBlockedAreaCenter = null;
        lastBlockedAreaAtMs = 0L;
        lastBlockedAreaLevel = 0;
        lastFiltered = null;
        prevFiltered = null;
        liveLocationCoordinator.reset();

        if (destination == null || profile == null || profile.trim().isEmpty()) {
            lastRouteFailureMessage = context.getString(R.string.nav_start_invalid_request);
            AppLogger.e(TAG, "Navigation start aborted because the request is incomplete profile="
                    + profile + " destination=" + formatLatLon(destination), null);
            return false;
        }
        return true;
    }

    void stop() {
        invalidatePendingRouteResults();
    }

    boolean hasActiveRoute() {
        return route != null;
    }

    void onProviderDisabled(@NonNull String provider) {
        liveLocationCoordinator.clearProvider(provider);
    }

    @Nullable
    Location getLastFilteredLocation() {
        return lastFiltered;
    }

    @NonNull
    NavigationRequest currentNavigationRequest() {
        return new NavigationRequest(profile, destinationName, destination, intermediates);
    }

    @NonNull
    LocationUpdateResult onRawLocationChanged(@NonNull Context context, @NonNull Location location, long nowMs) {
        liveLocationCoordinator.remember(location);
        Location selected = liveLocationCoordinator.selectBestLiveLocation();
        if (selected == null) {
            AppLogger.d(TAG, "Dropped location because no recent candidate is available raw="
                    + formatLocation(location));
            return LocationUpdateResult.dropped();
        }
        if (!liveLocationCoordinator.shouldDispatch(selected)) {
            AppLogger.d(TAG, "Dropped location because selected candidate is unchanged raw="
                    + formatLocation(location)
                    + " selected=" + formatLocation(selected));
            return LocationUpdateResult.dropped();
        }
        liveLocationCoordinator.markDispatched(selected);

        Location filtered = kalman.update(selected);
        if (filtered == null) {
            AppLogger.d(TAG, "Kalman filter dropped location " + formatLocation(selected));
            return LocationUpdateResult.dropped();
        }

        prevFiltered = lastFiltered;
        lastFiltered = filtered;
        lastRouteFailureMessage = null;
        locationUpdateCount++;
        AppLogger.d(TAG, "Location update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));

        if (destination == null || profile == null || profile.trim().isEmpty()) {
            lastRouteFailureMessage = context.getString(R.string.nav_start_invalid_request);
            AppLogger.e(TAG, "Skipping route evaluation because destination/profile is incomplete profile="
                    + profile + " destination=" + formatLatLon(destination), null);
            return LocationUpdateResult.accepted(filtered, true, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }

        if (route == null || polylineIndex == null || route.track.isEmpty()) {
            AppLogger.i(TAG, "No active route loaded, requesting route calculation");
            return LocationUpdateResult.accepted(filtered, true, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(filtered.getLatitude(), filtered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            AppLogger.w(TAG, "Route match failed, requesting recalculation");
            return LocationUpdateResult.accepted(filtered, true, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }
        lastSegmentIndex = match.segmentIndex;

        Double actualBearing = getActualBearingDegrees(filtered);
        RouteDeviationPolicy.Decision deviationDecision = routeDeviationPolicy.evaluate(
                match.distanceToTrackMeters,
                accuracyMeters(filtered),
                actualBearing,
                match.segmentBearingDegrees
        );
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.OFF_TRACK) {
            AppLogger.w(TAG, "Off-track detected distance=" + match.distanceToTrackMeters
                    + " threshold=" + deviationDecision.offTrackThresholdMeters);
            return LocationUpdateResult.accepted(filtered, true, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }
        if (deviationDecision.reason == RouteDeviationPolicy.Reason.BEARING_MISMATCH) {
            AppLogger.w(TAG, "Bearing mismatch detected diff=" + deviationDecision.bearingDiffDegrees
                    + " expected=" + match.segmentBearingDegrees
                    + " actual=" + actualBearing);
            return LocationUpdateResult.accepted(filtered, true, Collections.emptyList(), NO_SUGGESTED_INTERVAL);
        }

        float speedMps = getSpeedMps(filtered);
        TurnEventPlanner.Progress turnProgress = turnEventPlanner.advance(
                route.voiceHints,
                polylineIndex,
                nextHintIdx,
                notified10,
                notified5,
                match.alongTrackMeters,
                speedMps
        );
        nextHintIdx = turnProgress.nextHintIdx;
        notified10 = turnProgress.notified10;
        notified5 = turnProgress.notified5;
        List<TurnEvent> turnEvents = toTurnEvents(turnProgress.signals);
        long suggestedIntervalMs = updateScheduler.suggestUpdateInterval(
                nowMs,
                fastChecksUntilMs,
                route.voiceHints,
                polylineIndex,
                nextHintIdx,
                match.alongTrackMeters,
                speedMps
        );
        return LocationUpdateResult.accepted(filtered, false, turnEvents, suggestedIntervalMs);
    }

    @NonNull
    List<NogoPoint> addBlockedPointsAhead() {
        List<NogoPoint> added = new ArrayList<>();
        if (lastFiltered == null || route == null || polylineIndex == null || route.track.isEmpty()) {
            return added;
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return added;
        }

        LatLon anchor = polylineIndex.pointAtDistance(match.alongTrackMeters + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS);
        if (anchor == null) {
            return added;
        }

        int level = nextBlockedAreaLevel(anchor, System.currentTimeMillis());
        double radiusMeters = blockedRadiusForLevel(level);
        int pointCount = blockedPointCountForLevel(level);
        replaceNearbyBlockedPoints(anchor);

        for (int i = 0; i < pointCount; i++) {
            double distance = match.alongTrackMeters
                    + BLOCKED_ROUTE_FIRST_POINT_OFFSET_METERS
                    + (i * BLOCKED_ROUTE_POINT_STEP_METERS);
            LatLon point = polylineIndex.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            NogoPoint nogo = new NogoPoint(point.lat, point.lon, radiusMeters);
            blocked.add(nogo);
            added.add(nogo);
        }

        lastBlockedAreaCenter = anchor;
        lastBlockedAreaAtMs = System.currentTimeMillis();
        lastBlockedAreaLevel = level;
        return added;
    }

    @Nullable
    RouteRequestSnapshot prepareRouteRequest(boolean force, long nowMs) {
        if (!force && nowMs - lastRerouteMs < 8000L) {
            AppLogger.d(TAG, "Skipping route recalculation because of throttle elapsedMs=" + (nowMs - lastRerouteMs));
            return null;
        }
        if (lastFiltered == null) {
            AppLogger.w(TAG, "Cannot recalculate route without a filtered location");
            return null;
        }

        lastRerouteMs = nowMs;
        int requestNumber = ++routeRequestCount;
        int requestToken = ++routeRequestToken;
        RouteRequestSnapshot snapshot = new RouteRequestSnapshot(
                requestNumber,
                requestToken,
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                new ArrayList<>(intermediates),
                destination,
                profile,
                new ArrayList<>(blocked)
        );
        routeCalculationInProgress = true;
        lastRouteFailureMessage = null;
        AppLogger.i(TAG, "Submitting route recalculation #" + requestNumber
                + " force=" + force
                + " start=" + formatLatLon(snapshot.start)
                + " destination=" + formatLatLon(destination)
                + " intermediates=" + snapshot.intermediates.size()
                + " blocked=" + snapshot.blocked.size());
        return snapshot;
    }

    @NonNull
    List<TurnEvent> applyRouteResult(
            @NonNull Context context,
            @NonNull RouteRequestSnapshot snapshot,
            @NonNull GeoJsonRoute newRoute,
            long beganAt
    ) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route result #" + snapshot.requestNumber);
            return Collections.emptyList();
        }
        route = newRoute;
        polylineIndex = new PolylineIndex(newRoute.track);
        lastSegmentIndex = -1;
        nextHintIdx = findNextHintIndex(newRoute, polylineIndex, lastFiltered);
        notified10 = false;
        notified5 = false;
        initialTurnNotificationSent = false;
        targets = buildTargets(context, polylineIndex);
        routeCalculationInProgress = false;
        lastRouteFailureMessage = null;

        List<TurnEvent> turnEvents = buildInitialTurnEventIfNeeded();
        AppLogger.i(TAG, "Route recalculation #" + snapshot.requestNumber
                + " succeeded durationMs=" + (System.currentTimeMillis() - beganAt)
                + " trackPoints=" + newRoute.track.size()
                + " voiceHints=" + newRoute.voiceHints.size()
                + " lengthMeters=" + newRoute.trackLengthMeters);
        return turnEvents;
    }

    void applyRouteFailure(
            @NonNull Context context,
            @NonNull RouteRequestSnapshot snapshot,
            @NonNull Exception error
    ) {
        if (snapshot.requestToken != routeRequestToken) {
            AppLogger.d(TAG, "Discarded stale route failure #" + snapshot.requestNumber);
            return;
        }
        routeCalculationInProgress = false;
        lastRouteFailureMessage = summarizeRouteFailure(context, error);
        AppLogger.e(TAG, "Route recalculation #" + snapshot.requestNumber + " failed", error);
        AppLogger.w(TAG, "Route recalculation #" + snapshot.requestNumber + " failure summary="
                + lastRouteFailureMessage);
    }

    @NonNull
    NavState buildState(@NonNull Context context, long nextEvaluationDeadlineElapsedMs, long nowMs) {
        if (lastFiltered == null) {
            if (lastRouteFailureMessage != null) {
                return NavState.routeUnavailable(context, lastRouteFailureMessage, nextEvaluationDeadlineElapsedMs);
            }
            return NavState.waitingForLocation(context, nextEvaluationDeadlineElapsedMs);
        }

        if (routeCalculationInProgress) {
            return NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        }

        if (route == null || polylineIndex == null) {
            if (lastRouteFailureMessage != null) {
                return NavState.routeUnavailable(context, lastRouteFailureMessage, nextEvaluationDeadlineElapsedMs);
            }
            return NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavState.waiting(context);
        }

        return NavState.from(
                route,
                polylineIndex,
                match.alongTrackMeters,
                nextHintIdx,
                getSpeedMps(lastFiltered),
                accuracyMeters(lastFiltered),
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
    }

    @NonNull
    private List<TurnEvent> buildInitialTurnEventIfNeeded() {
        if (initialTurnNotificationSent || route == null || polylineIndex == null) {
            return Collections.emptyList();
        }
        List<VoiceHint> hints = route.voiceHints;
        if (hints.isEmpty() || nextHintIdx < 0 || nextHintIdx >= hints.size()) {
            return Collections.emptyList();
        }

        double alongTrackMeters = 0.0;
        if (lastFiltered != null) {
            PolylineIndex.Match match = polylineIndex.match(
                    new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                    -1
            );
            if (match != null) {
                alongTrackMeters = match.alongTrackMeters;
            }
        }

        float speedMps = lastFiltered != null ? getSpeedMps(lastFiltered) : 0f;
        TurnEventPlanner.TurnSignal initialSignal = turnEventPlanner.buildInitialSignal(
                hints,
                polylineIndex,
                nextHintIdx,
                initialTurnNotificationSent,
                alongTrackMeters,
                speedMps
        );
        if (initialSignal == null) {
            return Collections.emptyList();
        }
        initialTurnNotificationSent = true;
        return Collections.singletonList(toTurnEvent(initialSignal));
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
    private List<TurnEvent> toTurnEvents(@NonNull List<TurnEventPlanner.TurnSignal> signals) {
        if (signals.isEmpty()) {
            return Collections.emptyList();
        }
        List<TurnEvent> events = new ArrayList<>(signals.size());
        for (TurnEventPlanner.TurnSignal signal : signals) {
            events.add(toTurnEvent(signal));
        }
        return events;
    }

    @NonNull
    private TurnEvent toTurnEvent(@NonNull TurnEventPlanner.TurnSignal signal) {
        switch (signal.type) {
            case PASSED:
                return TurnEvent.passed(signal.hint);
            case INITIAL:
                return TurnEvent.initial(signal.hint, signal.distanceMeters, signal.timeSeconds);
            case IMMINENT:
            default:
                return TurnEvent.imminent(signal.hint, signal.distanceMeters, signal.timeSeconds);
        }
    }

    @NonNull
    private List<NavTarget> buildTargets(@NonNull Context context, @NonNull PolylineIndex index) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            PolylineIndex.Match match = index.match(intermediates.get(i), -1);
            if (match != null) {
                out.add(new NavTarget(context.getString(R.string.format_stop_label, i + 1), match.alongTrackMeters));
            }
        }
        out.add(new NavTarget(context.getString(R.string.label_destination), index.totalLengthMeters()));
        return out;
    }

    private float getSpeedMps(@NonNull Location location) {
        if (location.hasSpeed()) {
            return Math.max(0f, location.getSpeed());
        }
        if (prevFiltered != null) {
            double distanceMeters = GeoMath.distanceMeters(
                    prevFiltered.getLatitude(),
                    prevFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            double deltaSeconds = Math.max(1.0, (location.getTime() - prevFiltered.getTime()) / 1000.0);
            return (float) (distanceMeters / deltaSeconds);
        }
        return 0f;
    }

    @Nullable
    private Double getActualBearingDegrees(@NonNull Location location) {
        if (location.hasBearing() && getSpeedMps(location) > 1.0f) {
            return (double) location.getBearing();
        }
        if (prevFiltered != null) {
            double distanceMeters = GeoMath.distanceMeters(
                    prevFiltered.getLatitude(),
                    prevFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            if (distanceMeters < 3.0) {
                return null;
            }
            return GeoMath.bearingDegrees(
                    prevFiltered.getLatitude(),
                    prevFiltered.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
        }
        return null;
    }

    private float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }

    private int nextBlockedAreaLevel(@NonNull LatLon anchor, long nowMs) {
        if (lastBlockedAreaCenter == null || lastBlockedAreaLevel <= 0) {
            return 1;
        }
        double distanceMeters = GeoMath.distanceMeters(
                lastBlockedAreaCenter.lat,
                lastBlockedAreaCenter.lon,
                anchor.lat,
                anchor.lon
        );
        boolean sameArea = distanceMeters <= BLOCKED_SAME_AREA_METERS;
        boolean quickNearbyRepeat = nowMs - lastBlockedAreaAtMs <= BLOCKED_QUICK_REPEAT_WINDOW_MS
                && distanceMeters <= BLOCKED_QUICK_REPEAT_NEARBY_METERS;
        if (sameArea || quickNearbyRepeat) {
            return Math.min(BLOCKED_POINT_COUNT_MAX, lastBlockedAreaLevel + 1);
        }
        return 1;
    }

    private void replaceNearbyBlockedPoints(@NonNull LatLon anchor) {
        blocked.removeIf(existing ->
                GeoMath.distanceMeters(existing.lat, existing.lon, anchor.lat, anchor.lon)
                        <= BLOCKED_QUICK_REPEAT_NEARBY_METERS
        );
    }

    private int blockedPointCountForLevel(int level) {
        return Math.max(1, Math.min(BLOCKED_POINT_COUNT_MAX, level));
    }

    private double blockedRadiusForLevel(int level) {
        return Math.min(
                BLOCKED_RADIUS_MAX_METERS,
                BLOCKED_RADIUS_BASE_METERS + ((Math.max(1, level) - 1) * BLOCKED_RADIUS_STEP_METERS)
        );
    }

    private void invalidatePendingRouteResults() {
        routeRequestToken++;
        routeCalculationInProgress = false;
    }

    @NonNull
    private String summarizeRouteFailure(@NonNull Context context, @NonNull Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String sanitized = message.replace('\r', ' ').replace('\n', ' ').trim();
                if (!sanitized.isEmpty()) {
                    return sanitized.length() > 120 ? sanitized.substring(0, 117) + "..." : sanitized;
                }
            }
            current = current.getCause();
        }
        return context.getString(R.string.nav_route_unavailable_generic);
    }

    private static final long NO_SUGGESTED_INTERVAL = -1L;

    static final class LocationUpdateResult {
        private final boolean dropped;
        private final boolean shouldRecalculateRoute;
        private final long suggestedUpdateIntervalMs;
        @Nullable
        final Location filteredLocation;
        @NonNull
        final List<TurnEvent> turnEvents;

        private LocationUpdateResult(
                boolean dropped,
                boolean shouldRecalculateRoute,
                long suggestedUpdateIntervalMs,
                @Nullable Location filteredLocation,
                @NonNull List<TurnEvent> turnEvents
        ) {
            this.dropped = dropped;
            this.shouldRecalculateRoute = shouldRecalculateRoute;
            this.suggestedUpdateIntervalMs = suggestedUpdateIntervalMs;
            this.filteredLocation = filteredLocation;
            this.turnEvents = turnEvents;
        }

        @NonNull
        static LocationUpdateResult dropped() {
            return new LocationUpdateResult(true, false, NO_SUGGESTED_INTERVAL, null, Collections.emptyList());
        }

        @NonNull
        static LocationUpdateResult accepted(
                @Nullable Location filteredLocation,
                boolean shouldRecalculateRoute,
                @NonNull List<TurnEvent> turnEvents,
                long suggestedUpdateIntervalMs
        ) {
            return new LocationUpdateResult(
                    false,
                    shouldRecalculateRoute,
                    suggestedUpdateIntervalMs,
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

        private RouteRequestSnapshot(
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

    @NonNull
    private static String formatLatLon(@Nullable LatLon value) {
        if (value == null) {
            return "null";
        }
        return value.lat + "," + value.lon;
    }

    @NonNull
    private static String formatLocation(@Nullable Location location) {
        if (location == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(location.getProvider())
                .append("(")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude())
                .append(")");
        if (location.hasAccuracy()) {
            sb.append(" acc=").append(location.getAccuracy());
        }
        if (location.hasSpeed()) {
            sb.append(" speed=").append(location.getSpeed());
        }
        if (location.hasBearing()) {
            sb.append(" bearing=").append(location.getBearing());
        }
        sb.append(" time=").append(location.getTime());
        return sb.toString();
    }
}
