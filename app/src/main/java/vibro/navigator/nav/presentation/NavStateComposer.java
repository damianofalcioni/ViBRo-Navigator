package vibro.navigator.nav.presentation;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSpeedLimit;

public final class NavStateComposer {
    private NavStateComposer() {
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return create(noRoute, "", "", "", defaultGpsStatusLine(context), NavState.NO_DEADLINE, noRoute, null, false);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NavState.NO_DEADLINE);
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
        return calculatingRoute(context, NavState.NO_DEADLINE);
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
        return routeUnavailable(context, detail, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState routeUnavailable(
            @NonNull Context context,
            @NonNull String detail,
            long nextEvaluationDeadlineElapsedMs
    ) {
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
    public static NavState from(@NonNull NavStateBuildInput input) {
        List<String> directionLines = NavStateTextFactory.buildDirectionLines(
                input.route,
                input.index,
                input.routeProgress.alongTrackMeters,
                input.routeProgress.nextHintIndex,
                input.routeProgress.currentSegmentIndex,
                input.motion.etaSpeedMps,
                input.motion.accuracyMeters,
                input.destinationReached,
                input.intermediateDestinationReachedTrackIndex,
                input.targets,
                input.context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
        String destination = NavStateTextFactory.buildDestinationLine(
                input.route,
                input.index,
                input.routeProgress.alongTrackMeters,
                input.routeProgress.currentSegmentIndex,
                input.motion.etaSpeedMps,
                input.timing.nowMs,
                input.destinationReached,
                input.targets,
                input.context
        );
        String stopProgress = NavStateTextFactory.buildStopProgress(
                input.route,
                input.index,
                input.routeProgress.alongTrackMeters,
                input.routeProgress.currentSegmentIndex,
                input.motion.etaSpeedMps,
                input.timing.nowMs,
                input.destinationReached,
                input.targets,
                input.context
        );
        String gpsStatus = buildGpsStatusLine(
                input.motion.speedMps,
                input.currentLocation,
                input.motion.accuracyMeters,
                input.gps.fixedSatelliteCount,
                input.gps.acquiredFixCount,
                input.context
        );
        NavCompassState compassState = input.compassInput == null
                ? null
                : NavCompassStateFactory.buildCompassState(input.compassInput);
        return create(
                next,
                afterNext,
                destination,
                stopProgress,
                gpsStatus,
                input.timing.nextEvaluationDeadlineElapsedMs,
                "",
                compassState,
                input.route.speedLimitAt(input.routeProgress.alongTrackMeters),
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
    public static NavState withGuidanceLines(
            @NonNull NavState base,
            @NonNull String nextLine,
            @NonNull String afterNextLine
    ) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(nextLine, afterNextLine),
                        base.routeStatus.progress,
                        base.routeStatus.compassState,
                        base.routeStatus.speedLimit
                ),
                base.gpsStatus,
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
    static NavState create(
            @NonNull String nextLine,
            @NonNull String afterNextLine,
            @NonNull String destinationLine,
            @NonNull String stopProgressBlock,
            @NonNull String gpsStatusLine,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull String detailBlock,
            @Nullable NavCompassState compassState,
            boolean paused
    ) {
        return create(
                nextLine,
                afterNextLine,
                destinationLine,
                stopProgressBlock,
                gpsStatusLine,
                nextEvaluationDeadlineElapsedMs,
                detailBlock,
                compassState,
                null,
                paused
        );
    }

    @NonNull
    private static NavState create(
            @NonNull String nextLine,
            @NonNull String afterNextLine,
            @NonNull String destinationLine,
            @NonNull String stopProgressBlock,
            @NonNull String gpsStatusLine,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull String detailBlock,
            @Nullable NavCompassState compassState,
            @Nullable RouteSpeedLimit speedLimit,
            boolean paused
    ) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(nextLine, afterNextLine),
                        new NavProgressStatus(destinationLine, stopProgressBlock, detailBlock),
                        compassState,
                        speedLimit
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
    public static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull List<LatLon> intermediateStops
    ) {
        return NavCompassStateFactory.buildCompassRouteGeometry(route, index, intermediateStops);
    }

    @NonNull
    public static String buildGpsStatusLine(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull Context context
    ) {
        return NavCompassStateFactory.buildGpsStatusLine(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount,
                context
        );
    }

    @NonNull
    private static String defaultGpsStatusLine(@NonNull Context context) {
        return buildGpsStatusLine(Float.NaN, null, Float.NaN, null, null, context);
    }
}
