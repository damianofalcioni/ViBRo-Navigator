package com.vibenavigator.nav;

import android.content.Context;

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

    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    @NonNull
    public final String destinationLine;
    @NonNull
    public final String accuracyLine;
    public final long nextEvaluationDeadlineElapsedMs;
    @NonNull
    public final String remainingBlock;
    @Nullable
    public final NavCompassState compassState;

    private NavState(@NonNull String nextLine,
                     @NonNull String afterNextLine,
                     @NonNull String destinationLine,
                     @NonNull String accuracyLine,
                     long nextEvaluationDeadlineElapsedMs,
                     @NonNull String remainingBlock,
                     @Nullable NavCompassState compassState) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.destinationLine = destinationLine;
        this.accuracyLine = accuracyLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.remainingBlock = remainingBlock;
        this.compassState = compassState;
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return new NavState(noRoute, "", "", "", NO_DEADLINE, noRoute, null);
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
        String remaining = base.remainingBlock.isEmpty()
                ? notice
                : notice + "\n" + base.remainingBlock;
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.accuracyLine,
                base.nextEvaluationDeadlineElapsedMs,
                remaining,
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
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
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
        String accuracy = buildAccuracyLine(accuracyMeters, context);
        String remaining = buildRemaining(route, index, alongTrackMeters, speedMps, nowMs, targets, context);
        NavCompassState compassState = buildCompassState(
                route,
                index,
                currentLocation,
                alongTrackMeters,
                speedMps,
                accuracyMeters,
                headingDegrees,
                headingAccuracyDegrees
        );
        return new NavState(next, afterNext, destination, accuracy, nextEvaluationDeadlineElapsedMs, remaining, compassState);
    }

    @NonNull
    private static String buildAccuracyLine(float accuracyMeters, @NonNull Context context) {
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_accuracy_value, accuracyMeters);
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
    private static String buildRemaining(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        List<String> lines = new ArrayList<>();
        double total = index.totalLengthMeters();

        int lastStopIndex = Math.max(0, targets.size() - 1);
        for (int i = 0; i < lastStopIndex; i++) {
            NavTarget t = targets.get(i);
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            double secTo = estimateSeconds(route, total, distTo, speedMps);
            String line = context.getString(
                    R.string.format_progress_line,
                    t.label,
                    NavigationTextFormatter.formatDistance(context, distTo),
                    NavigationTextFormatter.formatTimeSeconds(context, (int) Math.round(secTo)),
                    context.getString(R.string.nav_eta),
                    NavigationTextFormatter.formatEta(nowMs + (long) (secTo * 1000))
            );
            lines.add(line);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    @Nullable
    private static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull Location currentLocation,
            double alongTrackMeters,
            float speedMps,
            float accuracyMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees
    ) {
        if (route.track.isEmpty()) {
            return null;
        }

        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        double routeStart = Math.max(0.0, alongTrackMeters - 120.0);
        double routeEnd = Math.min(index.totalLengthMeters(), alongTrackMeters + 420.0);
        double stepMeters = 12.0;
        List<NavCompassState.RoutePoint> points = new ArrayList<>();
        List<NavCompassState.RoutePoint> hintPoints = new ArrayList<>();
        double furthestDistanceMeters = 0.0;

        for (double distance = routeStart; distance <= routeEnd; distance += stepMeters) {
            LatLon point = index.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            float eastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon);
            float northMeters = (float) GeoMath.northMeters(currentLat, point.lat);
            points.add(new NavCompassState.RoutePoint(eastMeters, northMeters));
            furthestDistanceMeters = Math.max(
                    furthestDistanceMeters,
                    Math.hypot(eastMeters, northMeters)
            );
        }

        for (VoiceHint hint : route.voiceHints) {
            double hintDistance = index.distanceAtPointIndex(hint.indexInTrack);
            if (hintDistance < routeStart || hintDistance > routeEnd) {
                continue;
            }
            LatLon hintPoint = index.pointAtDistance(hintDistance);
            if (hintPoint == null) {
                continue;
            }
            float eastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, hintPoint.lat, hintPoint.lon);
            float northMeters = (float) GeoMath.northMeters(currentLat, hintPoint.lat);
            hintPoints.add(new NavCompassState.RoutePoint(eastMeters, northMeters));
        }

        LatLon routeEndPoint = route.track.get(route.track.size() - 1);
        float destinationEastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, routeEndPoint.lat, routeEndPoint.lon);
        float destinationNorthMeters = (float) GeoMath.northMeters(currentLat, routeEndPoint.lat);
        double destinationDistanceMeters = Math.hypot(destinationEastMeters, destinationNorthMeters);
        furthestDistanceMeters = Math.max(furthestDistanceMeters, destinationDistanceMeters);

        float visibleRadiusMeters = (float) Math.max(90.0, Math.min(320.0, furthestDistanceMeters * 1.15));
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return new NavCompassState(
                resolvedHeading,
                sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                sanitizeReferenceSpeedMps(speedMps),
                visibleRadiusMeters,
                sanitizeAccuracyMeters(accuracyMeters),
                points,
                hintPoints,
                destinationEastMeters,
                destinationNorthMeters,
                destinationDistanceMeters <= visibleRadiusMeters
        );
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
}
