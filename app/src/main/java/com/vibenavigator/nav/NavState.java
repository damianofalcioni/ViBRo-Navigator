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
    public final boolean paused;

    private NavState(@NonNull String nextLine,
                     @NonNull String afterNextLine,
                     @NonNull String destinationLine,
                     @NonNull String stopProgressBlock,
                     @NonNull String gpsStatusLine,
                     long nextEvaluationDeadlineElapsedMs,
                     @NonNull String detailBlock,
                     @Nullable NavCompassState compassState,
                     boolean paused) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.destinationLine = destinationLine;
        this.stopProgressBlock = stopProgressBlock;
        this.gpsStatusLine = gpsStatusLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.detailBlock = detailBlock;
        this.compassState = compassState;
        this.paused = paused;
    }

    @NonNull
    public String displayStatusBlock() {
        if (!detailBlock.isEmpty()) {
            return detailBlock;
        }
        if (destinationLine.isEmpty()) {
            return stopProgressBlock;
        }
        if (stopProgressBlock.isEmpty()) {
            return destinationLine;
        }
        return destinationLine + "\n" + stopProgressBlock;
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return new NavState(noRoute, "", "", "", defaultGpsStatusLine(context), NO_DEADLINE, noRoute, null, false);
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
                null,
                false
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
                null,
                false
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
                null,
                false
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
                base.compassState,
                base.paused
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
                base.compassState,
                base.paused
        );
    }

    @NonNull
    public static NavState withPauseState(@NonNull Context context, @NonNull NavState base, boolean paused) {
        String detail = base.detailBlock;
        if (paused) {
            String pauseNotice = context.getString(R.string.nav_paused_notice);
            detail = detail.isEmpty() ? pauseNotice : pauseNotice + "\n" + detail;
        }
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                base.gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                detail,
                base.compassState,
                paused
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                speedMps,
                likelyStationary,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
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
                currentSegmentIndex,
                etaSpeedMps,
                accuracyMeters,
                context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
        String destination = buildDestinationLine(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                etaSpeedMps,
                nowMs,
                targets,
                context
        );
        String stopProgress = buildStopProgress(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                etaSpeedMps,
                nowMs,
                targets,
                context
        );
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
                previousReliableMovingCompassVisibleRadiusMeters,
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
                compassState,
                false
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
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            @NonNull Context context
    ) {
        if (route.voiceHints.isEmpty() || hintIdx < 0 || hintIdx >= route.voiceHints.size()) {
            return new ArrayList<>();
        }
        List<UpcomingHint> upcomingHints = new ArrayList<>(2);
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = hintIdx; i < route.voiceHints.size() && upcomingHints.size() < 2; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            if (dist <= minReliableDistanceMeters) {
                continue;
            }
            Double timeSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    hint.indexInTrack,
                    speedMps
            );
            upcomingHints.add(new UpcomingHint(
                    hint,
                    dist,
                    timeSeconds != null ? timeSeconds : Double.NaN
            ));
        }
        List<String> lines = new ArrayList<>(upcomingHints.size());
        if (upcomingHints.isEmpty()) {
            return lines;
        }
        UpcomingHint nextHint = upcomingHints.get(0);
        lines.add(NavigationTextFormatter.formatTurnNotification(
                context,
                nextHint.hint,
                nextHint.distanceMeters,
                nextHint.timeSeconds
        ));
        if (upcomingHints.size() > 1) {
            UpcomingHint afterNextHint = upcomingHints.get(1);
            double relativeDistanceMeters = Math.max(
                    0.0,
                    afterNextHint.distanceMeters - nextHint.distanceMeters
            );
            double relativeTimeSeconds = Double.isFinite(nextHint.timeSeconds)
                    && Double.isFinite(afterNextHint.timeSeconds)
                    ? Math.max(0.0, afterNextHint.timeSeconds - nextHint.timeSeconds)
                    : resolveRelativeHintTimeSeconds(route, index, nextHint, afterNextHint, speedMps);
            lines.add(NavigationTextFormatter.formatTurnNotification(
                    context,
                    afterNextHint.hint,
                    relativeDistanceMeters,
                    relativeTimeSeconds
            ));
        }
        return lines;
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
    }

    private static double resolveRelativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull UpcomingHint nextHint,
            @NonNull UpcomingHint afterNextHint,
            float speedMps
    ) {
        Double estimatedSeconds = RouteTimeEstimator.estimateSecondsBetweenTrackPoints(
                route,
                index,
                nextHint.hint.indexInTrack,
                afterNextHint.hint.indexInTrack
        );
        return estimatedSeconds != null ? estimatedSeconds : Double.NaN;
    }

    @NonNull
    private static String buildDestinationLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (targets.isEmpty()) {
            return "";
        }
        NavTarget destination = targets.get(targets.size() - 1);
        double distTo = Math.max(0.0, destination.alongTrackMeters - alongTrackMeters);
        Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                destination.alongTrackMeters,
                speedMps
        );
        return buildProgressLine(context, destination.label, distTo, secTo, nowMs);
    }

    @NonNull
    private static String buildStopProgress(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        int lastStopIndex = Math.max(0, targets.size() - 1);
        for (int i = 0; i < lastStopIndex; i++) {
            NavTarget t = targets.get(i);
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            if (distTo <= 0.0) {
                continue;
            }
            Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    t.alongTrackMeters,
                    speedMps
            );
            return buildProgressLine(context, t.label, distTo, secTo, nowMs);
        }
        return "";
    }

    @NonNull
    private static String buildProgressLine(
            @NonNull Context context,
            @NonNull String label,
            double distanceMeters,
            @Nullable Double seconds,
            long nowMs
    ) {
        String timeText = NavigationTextFormatter.formatTimeSeconds(
                context,
                seconds != null ? seconds : Double.NaN
        );
        String etaText = seconds != null && Double.isFinite(seconds)
                ? NavigationTextFormatter.formatEta(nowMs + (long) (seconds * 1000))
                : context.getString(R.string.nav_status_unavailable);
        return context.getString(
                R.string.format_progress_line,
                label,
                NavigationTextFormatter.formatDistance(context, distanceMeters),
                timeText,
                context.getString(R.string.nav_eta),
                etaText
        );
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
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
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
        boolean reliableMovingSpeed = hasReliableMovingSpeed(currentLocation, likelyStationary);
        boolean hasReusableMovingRadius = previousReliableMovingCompassVisibleRadiusMeters != null
                && Float.isFinite(previousReliableMovingCompassVisibleRadiusMeters)
                && previousReliableMovingCompassVisibleRadiusMeters > 0f;
        float targetVisibleRadiusMeters;
        if (likelyStationary) {
            targetVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        } else if (reliableMovingSpeed) {
            targetVisibleRadiusMeters = Math.min(
                    fullRouteVisibleRadiusMeters,
                    resolveMovingVisibleRadiusMeters(speedMps)
            );
        } else if (hasReusableMovingRadius) {
            targetVisibleRadiusMeters = Math.min(
                    fullRouteVisibleRadiusMeters,
                    previousReliableMovingCompassVisibleRadiusMeters
            );
        } else {
            targetVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        }
        float visibleRadiusMeters = hasReusableMovingRadius && !reliableMovingSpeed && !likelyStationary
                ? targetVisibleRadiusMeters
                : smoothVisibleRadiusMeters(
                        targetVisibleRadiusMeters,
                        previousCompassVisibleRadiusMeters,
                        compassRadiusUpdateDeltaMs
                );
        boolean usingMovingScale = !likelyStationary && (reliableMovingSpeed || hasReusableMovingRadius);
        float referenceSpeedMps = usingMovingScale
                ? resolveMovingLegendReferenceSpeedMps(visibleRadiusMeters)
                : sanitizeReferenceSpeedMps(speedMps);
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
        return Math.max(COMPASS_MIN_VISIBLE_RADIUS_METERS, targetRadiusMeters);
    }

    static boolean hasReliableMovingSpeed(
            @NonNull Location currentLocation,
            boolean likelyStationary
    ) {
        return !likelyStationary
                && currentLocation.hasSpeed()
                && Float.isFinite(currentLocation.getSpeed())
                && currentLocation.getSpeed() > 0f;
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

    private static final class UpcomingHint {
        @NonNull
        final VoiceHint hint;
        final double distanceMeters;
        final double timeSeconds;

        private UpcomingHint(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }
    }
}
