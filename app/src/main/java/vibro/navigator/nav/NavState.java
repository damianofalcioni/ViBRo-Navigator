package vibro.navigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

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
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                false,
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
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                etaSpeedMps,
                likelyStationary,
                accuracyMeters,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                false,
                targets,
                context
        );
    }

    @NonNull
    static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            float compassAccuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean destinationReached,
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
                destinationReached,
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
                destinationReached,
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
                destinationReached,
                targets,
                context
        );
        String gpsStatus = buildGpsStatusLine(speedMps, currentLocation, accuracyMeters, fixedSatelliteCount, context);
        NavCompassState compassState = NavCompassStateFactory.buildCompassState(
                route,
                index,
                alongTrackMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRouteGeometry,
                compassRadiusTransition,
                nowMs
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
    static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return NavCompassStateFactory.buildCompassRouteGeometry(route, index);
    }

    @NonNull
    static String buildGpsStatusLine(
            float speedMps,
            @Nullable Location currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @NonNull Context context
    ) {
        return NavCompassStateFactory.buildGpsStatusLine(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                context
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
            boolean destinationReached,
            @NonNull Context context
    ) {
        if (destinationReached) {
            return new ArrayList<>(java.util.Collections.singletonList(
                    NavigationTextFormatter.formatTurnNotification(
                            context,
                            new VoiceHint(route.track.size() - 1, 100, 0, 0.0, 0),
                            0.0,
                            0.0
                    )
            ));
        }
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
                    : resolveRelativeHintTimeSeconds(route, index, nextHint, afterNextHint);
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
            @NonNull UpcomingHint afterNextHint
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
            boolean destinationReached,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (destinationReached) {
            return context.getString(R.string.nav_destination_reached);
        }
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
            boolean destinationReached,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (destinationReached) {
            return "";
        }
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

    static boolean hasReliableMovingSpeed(
            @NonNull Location currentLocation,
            boolean likelyStationary
    ) {
        return NavCompassStateFactory.hasReliableMovingSpeed(currentLocation, likelyStationary);
    }

    static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        return NavCompassStateFactory.smoothVisibleRadiusMeters(
                targetVisibleRadiusMeters,
                previousVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs
        );
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
