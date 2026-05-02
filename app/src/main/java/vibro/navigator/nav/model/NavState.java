package vibro.navigator.nav.model;



import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import android.location.Location;

import java.util.List;

public final class NavState {
    public static final long NO_DEADLINE = -1L;

    @NonNull
    public final NavRouteStatus routeStatus;
    @NonNull
    public final NavGpsStatus gpsStatus;
    @NonNull
    public final NavPauseStatus pauseStatus;

    private NavState(
            @NonNull NavRouteStatus routeStatus,
            @NonNull NavGpsStatus gpsStatus,
            @NonNull NavPauseStatus pauseStatus
    ) {
        this.routeStatus = routeStatus;
        this.gpsStatus = gpsStatus;
        this.pauseStatus = pauseStatus;
    }

    @NonNull
    public String displayStatusBlock() {
        return routeStatus.displayStatusBlock();
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return create(noRoute, "", "", "", defaultGpsStatusLine(context), NO_DEADLINE, noRoute, null, false);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return create(
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
        return create(
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
        return create(
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
        String detail = base.routeStatus.progress.detailBlock.isEmpty()
                ? notice
                : notice + "\n" + base.routeStatus.progress.detailBlock;
        return new NavState(
                base.routeStatus.withProgress(base.routeStatus.progress.withDetailBlock(detail)),
                base.gpsStatus,
                base.pauseStatus
        );
    }

    @NonNull
    public static NavState withGpsStatus(@NonNull NavState base, @NonNull String gpsStatusLine) {
        return new NavState(
                base.routeStatus,
                base.gpsStatus.withStatusLine(gpsStatusLine),
                base.pauseStatus
        );
    }

    @NonNull
    public static NavState withPauseState(@NonNull Context context, @NonNull NavState base, boolean paused) {
        String detail = base.routeStatus.progress.detailBlock;
        if (paused) {
            String pauseNotice = context.getString(R.string.nav_paused_notice);
            detail = detail.isEmpty() ? pauseNotice : pauseNotice + "\n" + detail;
        }
        return new NavState(
                base.routeStatus.withProgress(base.routeStatus.progress.withDetailBlock(detail)),
                base.gpsStatus,
                new NavPauseStatus(paused)
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
        List<String> directionLines = NavStateTextFactory.buildDirectionLines(
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
        String destination = NavStateTextFactory.buildDestinationLine(
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
        String stopProgress = NavStateTextFactory.buildStopProgress(
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
        return create(
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
    private static NavState create(@NonNull String nextLine,
                                   @NonNull String afterNextLine,
                                   @NonNull String destinationLine,
                                   @NonNull String stopProgressBlock,
                                   @NonNull String gpsStatusLine,
                                   long nextEvaluationDeadlineElapsedMs,
                                   @NonNull String detailBlock,
                                   @Nullable NavCompassState compassState,
                                   boolean paused) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(nextLine, afterNextLine),
                        new NavProgressStatus(destinationLine, stopProgressBlock, detailBlock),
                        compassState
                ),
                new NavGpsStatus(gpsStatusLine, nextEvaluationDeadlineElapsedMs),
                new NavPauseStatus(paused)
        );
    }

    @NonNull
    public static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        return NavCompassStateFactory.buildCompassRouteGeometry(route, index);
    }

    @NonNull
    public static String buildGpsStatusLine(
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

    public static boolean hasReliableMovingSpeed(
            @NonNull Location currentLocation,
            boolean likelyStationary
    ) {
        return NavCompassStateFactory.hasReliableMovingSpeed(currentLocation, likelyStationary);
    }

    public static float smoothVisibleRadiusMeters(
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

}
