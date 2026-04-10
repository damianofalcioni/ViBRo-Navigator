package com.vibenavigator.nav;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.GeoMath;
import com.vibenavigator.R;
import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.GeoJsonRoute;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public final class NavState {
    public static final long NO_DEADLINE = -1L;
    private static final float COMPASS_MOVING_LOOKAHEAD_SECONDS = 60f;
    private static final float COMPASS_MIN_VISIBLE_RADIUS_METERS = 90f;
    private static final float COMPASS_MAX_MOVING_VISIBLE_RADIUS_METERS = 600f;
    private static final long COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS = 1_200L;
    private static final int MAX_COMPASS_ROUTE_POINTS = 240;

    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    @NonNull
    public final String destinationLine;
    @NonNull
    public final String stopProgressBlock;
    @NonNull
    public final String gpsStatusLine;
    public final long nextEvaluationDeadlineElapsedMs;
    @NonNull
    public final String detailBlock;
    @Nullable
    public final NavCompassState compassState;

    private NavState(@NonNull String nextLine,
                     @NonNull String afterNextLine,
                     @NonNull String destinationLine,
                     @NonNull String stopProgressBlock,
                     @NonNull String gpsStatusLine,
                     long nextEvaluationDeadlineElapsedMs,
                     @NonNull String detailBlock,
                     @Nullable NavCompassState compassState) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.destinationLine = destinationLine;
        this.stopProgressBlock = stopProgressBlock;
        this.gpsStatusLine = gpsStatusLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.detailBlock = detailBlock;
        this.compassState = compassState;
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return new NavState(noRoute, "", "", "", defaultGpsStatusLine(context), NO_DEADLINE, noRoute, null);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_waiting_for_location_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_waiting_for_location_body),
                null
        );
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context) {
        return calculatingRoute(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_calculating_route_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_calculating_route_body),
                null
        );
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context, @NonNull String detail) {
        return routeUnavailable(context, detail, NO_DEADLINE);
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context,
                                            @NonNull String detail,
                                            long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_route_unavailable_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.format_nav_route_unavailable_body, detail),
                null
        );
    }

    @NonNull
    public static NavState withNotice(@NonNull NavState base, @NonNull String notice) {
        if (notice.trim().isEmpty()) {
            return base;
        }
        String detail = base.detailBlock.isEmpty()
                ? notice
                : notice + "\n" + base.detailBlock;
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                base.gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                detail,
                base.compassState
        );
    }

    @NonNull
    public static NavState withGpsStatus(@NonNull NavState base, @NonNull String gpsStatusLine) {
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                base.detailBlock,
                base.compassState
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        List<String> directionLines = buildDirectionLines(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                speedMps,
                accuracyMeters,
                context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
        String destination = buildDestinationLine(route, index, alongTrackMeters, speedMps, nowMs, targets, context);
        String stopProgress = buildStopProgress(route, index, alongTrackMeters, speedMps, nowMs, targets, context);
        String gpsStatus = buildGpsStatusLine(speedMps, currentLocation, accuracyMeters, fixedSatelliteCount, context);
        NavCompassState compassState = buildCompassState(
                route,
                index,
                alongTrackMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                accuracyMeters,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs
        );
        return new NavState(
                next,
                afterNext,
                destination,
                stopProgress,
                gpsStatus,
                nextEvaluationDeadlineElapsedMs,
                "",
                compassState
        );
    }

    @NonNull
    static String buildGpsStatusLine(
            float speedMps,
            @Nullable Location currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @NonNull Context context
    ) {
        Double elevationMeters = currentLocation != null && currentLocation.hasAltitude()
                ? currentLocation.getAltitude()
                : null;
        Float bearingDegrees = currentLocation != null && currentLocation.hasBearing()
                ? currentLocation.getBearing()
                : null;
        Float bearingAccuracyDegrees = currentLocation != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
        return NavigationTextFormatter.formatGpsStatus(
                context,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount
        );
    }

    @NonNull
    private static String defaultGpsStatusLine(@NonNull Context context) {
        return buildGpsStatusLine(Float.NaN, null, Float.NaN, null, context);
    }

    @NonNull
    private static List<String> buildDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            float speedMps,
            float accuracyMeters,
            @NonNull Context context
    ) {
        if (route.voiceHints.isEmpty() || hintIdx < 0 || hintIdx >= route.voiceHints.size()) {
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>(2);
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = hintIdx; i < route.voiceHints.size() && lines.size() < 2; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            if (dist <= minReliableDistanceMeters) {
                continue;
            }
            double time = dist / Math.max(1.0, speedMps);
            lines.add(NavigationTextFormatter.formatTurnNotification(context, hint, dist, time));
        }
        return lines;
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
    }

    @NonNull
    private static String buildDestinationLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (targets.isEmpty()) {
            return "";
        }
        NavTarget destination = targets.get(targets.size() - 1);
        double total = index.totalLengthMeters();
        double distTo = Math.max(0.0, destination.alongTrackMeters - alongTrackMeters);
        double secTo = estimateSeconds(route, total, distTo, speedMps);
        return context.getString(
                R.string.format_progress_line,
                destination.label,
                NavigationTextFormatter.formatDistance(context, distTo),
                NavigationTextFormatter.formatTimeSeconds(context, (int) Math.round(secTo)),
                context.getString(R.string.nav_eta),
                NavigationTextFormatter.formatEta(nowMs + (long) (secTo * 1000))
        );
    }

    @NonNull
    private static String buildStopProgress(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        double total = index.totalLengthMeters();

        int lastStopIndex = Math.max(0, targets.size() - 1);
        for (int i = 0; i < lastStopIndex; i++) {
            NavTarget t = targets.get(i);
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            if (distTo <= 0.0) {
                continue;
            }
            double secTo = estimateSeconds(route, total, distTo, speedMps);
            return context.getString(
                    R.string.format_progress_line,
                    t.label,
                    NavigationTextFormatter.formatDistance(context, distTo),
                    NavigationTextFormatter.formatTimeSeconds(context, (int) Math.round(secTo)),
                    context.getString(R.string.nav_eta),
                    NavigationTextFormatter.formatEta(nowMs + (long) (secTo * 1000))
            );
        }
        return "";
    }

    @Nullable
    private static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull Location currentLocation,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        if (route.track.isEmpty()) {
            return null;
        }

        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        CompassTrackSample trackSample = sampleCompassTrack(route, index, alongTrackMeters, currentLat, currentLon);

        LatLon routeEndPoint = route.track.get(route.track.size() - 1);
        float destinationEastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, routeEndPoint.lat, routeEndPoint.lon);
        float destinationNorthMeters = (float) GeoMath.northMeters(currentLat, routeEndPoint.lat);
        double destinationDistanceMeters = Math.hypot(destinationEastMeters, destinationNorthMeters);
        double furthestDistanceMeters = Math.max(trackSample.furthestDistanceMeters, destinationDistanceMeters);

        float fullRouteVisibleRadiusMeters = (float) Math.max(
                COMPASS_MIN_VISIBLE_RADIUS_METERS,
                furthestDistanceMeters * 1.15
        );
        float movingVisibleRadiusMeters = resolveMovingVisibleRadiusMeters(speedMps);
        float targetVisibleRadiusMeters = likelyStationary
                ? fullRouteVisibleRadiusMeters
                : Math.min(fullRouteVisibleRadiusMeters, movingVisibleRadiusMeters);
        float visibleRadiusMeters = smoothVisibleRadiusMeters(
                targetVisibleRadiusMeters,
                previousCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs
        );
        float referenceSpeedMps = likelyStationary
                ? sanitizeReferenceSpeedMps(speedMps)
                : resolveMovingLegendReferenceSpeedMps(visibleRadiusMeters);
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return new NavCompassState(
                resolvedHeading,
                sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                referenceSpeedMps,
                visibleRadiusMeters,
                sanitizeAccuracyMeters(accuracyMeters),
                trackSample.passedRoutePoints,
                trackSample.routePoints,
                trackSample.hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationDistanceMeters <= visibleRadiusMeters
        );
    }

    @NonNull
    private static CompassTrackSample sampleCompassTrack(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            double currentLat,
            double currentLon
    ) {
        List<NavCompassState.RoutePoint> points = new ArrayList<>();
        List<NavCompassState.RoutePoint> passedPoints = new ArrayList<>();
        List<NavCompassState.RoutePoint> hintPoints = new ArrayList<>();
        double furthestDistanceMeters = 0.0;

        if (route.track.size() == 1) {
            LatLon onlyPoint = route.track.get(0);
            points.add(projectRoutePoint(currentLat, currentLon, onlyPoint));
        } else {
            double totalLengthMeters = index.totalLengthMeters();
            double stepMeters = Math.max(12.0, totalLengthMeters / MAX_COMPASS_ROUTE_POINTS);
            double clampedAlongTrackMeters = Math.max(0.0, Math.min(alongTrackMeters, totalLengthMeters));
            furthestDistanceMeters = addSampledRoutePoints(
                    index,
                    currentLat,
                    currentLon,
                    0.0,
                    clampedAlongTrackMeters,
                    stepMeters,
                    passedPoints,
                    furthestDistanceMeters
            );
            furthestDistanceMeters = addSampledRoutePoints(
                    index,
                    currentLat,
                    currentLon,
                    clampedAlongTrackMeters,
                    totalLengthMeters,
                    stepMeters,
                    points,
                    furthestDistanceMeters
            );
        }

        for (VoiceHint hint : route.voiceHints) {
            LatLon hintPoint = index.pointAtDistance(index.distanceAtPointIndex(hint.indexInTrack));
            if (hintPoint == null) {
                continue;
            }
            hintPoints.add(projectRoutePoint(currentLat, currentLon, hintPoint));
        }

        return new CompassTrackSample(passedPoints, points, hintPoints, furthestDistanceMeters);
    }

    private static double addSampledRoutePoints(
            @NonNull PolylineIndex index,
            double currentLat,
            double currentLon,
            double startMeters,
            double endMeters,
            double stepMeters,
            @NonNull List<NavCompassState.RoutePoint> target,
            double furthestDistanceMeters
    ) {
        if (endMeters < startMeters) {
            return furthestDistanceMeters;
        }
        for (double distance = startMeters; distance < endMeters; distance += stepMeters) {
            LatLon point = index.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            furthestDistanceMeters = addProjectedRoutePoint(currentLat, currentLon, point, target, furthestDistanceMeters);
        }
        LatLon endPoint = index.pointAtDistance(endMeters);
        if (endPoint != null) {
            furthestDistanceMeters = addProjectedRoutePoint(currentLat, currentLon, endPoint, target, furthestDistanceMeters);
        }
        return furthestDistanceMeters;
    }

    private static double addProjectedRoutePoint(
            double currentLat,
            double currentLon,
            @NonNull LatLon point,
            @NonNull List<NavCompassState.RoutePoint> target,
            double furthestDistanceMeters
    ) {
        NavCompassState.RoutePoint routePoint = projectRoutePoint(currentLat, currentLon, point);
        target.add(routePoint);
        return Math.max(
                furthestDistanceMeters,
                Math.hypot(routePoint.eastMeters, routePoint.northMeters)
        );
    }

    @NonNull
    private static NavCompassState.RoutePoint projectRoutePoint(
            double currentLat,
            double currentLon,
            @NonNull LatLon point
    ) {
        return new NavCompassState.RoutePoint(
                (float) GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon),
                (float) GeoMath.northMeters(currentLat, point.lat)
        );
    }

    private static float resolveMovingVisibleRadiusMeters(float speedMps) {
        float safeSpeedMps = Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
        float targetRadiusMeters = safeSpeedMps * COMPASS_MOVING_LOOKAHEAD_SECONDS;
        return Math.max(
                COMPASS_MIN_VISIBLE_RADIUS_METERS,
                Math.min(COMPASS_MAX_MOVING_VISIBLE_RADIUS_METERS, targetRadiusMeters)
        );
    }

    private static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        if (previousVisibleRadiusMeters == null
                || !Float.isFinite(previousVisibleRadiusMeters)
                || previousVisibleRadiusMeters <= 0f
                || compassRadiusUpdateDeltaMs <= 0L) {
            return targetVisibleRadiusMeters;
        }
        long boundedDeltaMs = Math.min(compassRadiusUpdateDeltaMs, 5_000L);
        double alpha = 1.0 - Math.exp(-boundedDeltaMs / (double) COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS);
        return (float) (previousVisibleRadiusMeters
                + (targetVisibleRadiusMeters - previousVisibleRadiusMeters) * alpha);
    }

    private static float resolveMovingLegendReferenceSpeedMps(float visibleRadiusMeters) {
        float safeRadiusMeters = Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : COMPASS_MIN_VISIBLE_RADIUS_METERS;
        return Math.max(1f, safeRadiusMeters / COMPASS_MOVING_LOOKAHEAD_SECONDS);
    }

    private static float sanitizeAccuracyMeters(float accuracyMeters) {
        return Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters : 0f;
    }

    private static float sanitizeReferenceSpeedMps(float speedMps) {
        return Float.isFinite(speedMps) && speedMps > 0f ? Math.max(1f, speedMps) : 1f;
    }

    @Nullable
    private static Float sanitizeHeadingAccuracyDegrees(@Nullable Float headingAccuracyDegrees) {
        return headingAccuracyDegrees != null
                && Float.isFinite(headingAccuracyDegrees)
                && headingAccuracyDegrees > 0f
                ? headingAccuracyDegrees
                : null;
    }

    private static float normalizeHeading(double headingDegrees) {
        double normalized = headingDegrees % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return (float) normalized;
    }

    private static double estimateSeconds(@NonNull GeoJsonRoute route, double totalMeters, double remainingMeters, float speedMps) {
        if (speedMps >= 1.0f) {
            return remainingMeters / speedMps;
        }
        if (route.totalTimeSeconds > 0.0 && totalMeters > 0.0) {
            return route.totalTimeSeconds * (remainingMeters / totalMeters);
        }
        return 0.0;
    }

    private static final class CompassTrackSample {
        @NonNull
        final List<NavCompassState.RoutePoint> passedRoutePoints;
        @NonNull
        final List<NavCompassState.RoutePoint> routePoints;
        @NonNull
        final List<NavCompassState.RoutePoint> hintPoints;
        final double furthestDistanceMeters;

        private CompassTrackSample(
                @NonNull List<NavCompassState.RoutePoint> passedRoutePoints,
                @NonNull List<NavCompassState.RoutePoint> routePoints,
                @NonNull List<NavCompassState.RoutePoint> hintPoints,
                double furthestDistanceMeters
        ) {
            this.passedRoutePoints = passedRoutePoints;
            this.routePoints = routePoints;
            this.hintPoints = hintPoints;
            this.furthestDistanceMeters = furthestDistanceMeters;
        }
    }
}
