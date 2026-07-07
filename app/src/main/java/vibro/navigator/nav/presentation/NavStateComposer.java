package vibro.navigator.nav.presentation;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGpsTelemetry;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSpeedLimit;

public final class NavStateComposer {
    private NavStateComposer() {
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        return NavStateResourceComposer.waiting(new AndroidNavigationTextResources(context));
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return NavStateResourceComposer.waitingForLocation(
                new AndroidNavigationTextResources(context),
                nextEvaluationDeadlineElapsedMs
        );
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context) {
        return calculatingRoute(context, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return NavStateResourceComposer.calculatingRoute(
                new AndroidNavigationTextResources(context),
                nextEvaluationDeadlineElapsedMs
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
        return NavStateResourceComposer.routeUnavailable(
                new AndroidNavigationTextResources(context),
                detail,
                nextEvaluationDeadlineElapsedMs
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
                input.textResources
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
                input.textResources
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
                input.textResources
        );
        NavGpsTelemetry gpsTelemetry = NavigationGpsTelemetryFormatter.format(
                input.textResources,
                input.motion.speedMps,
                input.currentLocation,
                input.motion.accuracyMeters,
                input.gps.fixedSatelliteCount,
                input.gps.acquiredFixCount
        );
        NavCompassState compassState = input.compassInput == null
                ? null
                : NavCompassStateFactory.buildCompassState(input.compassInput);
        return create(
                next,
                afterNext,
                destination,
                stopProgress,
                gpsTelemetry.compactLine,
                input.timing.nextEvaluationDeadlineElapsedMs,
                "",
                compassState,
                input.route.speedLimitAt(input.routeProgress.alongTrackMeters),
                false,
                gpsTelemetry
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
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withGpsStatus(@NonNull NavState base, @NonNull String gpsStatusLine) {
        return new NavState(
                base.routeStatus,
                base.gpsStatus.withStatusLine(gpsStatusLine),
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withGpsStatus(@NonNull NavState base, @NonNull NavGpsStatus gpsStatus) {
        return new NavState(
                base.routeStatus,
                gpsStatus,
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withTripStatus(@NonNull NavState base, @NonNull NavTripStatus tripStatus) {
        return new NavState(
                base.routeStatus,
                base.gpsStatus,
                base.pauseStatus,
                tripStatus
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
                        base.routeStatus.speedLimit,
                        base.routeStatus.blockedRoadActionAvailable
                ),
                base.gpsStatus,
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withBlockedRoadActionAvailable(@NonNull NavState base, boolean available) {
        return new NavState(
                new NavRouteStatus(
                        base.routeStatus.guidance,
                        base.routeStatus.progress,
                        base.routeStatus.compassState,
                        base.routeStatus.speedLimit,
                        available
                ),
                base.gpsStatus,
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withCompassStreetOverlay(
            @NonNull NavState base,
            @NonNull CompassStreetOverlay streetOverlay
    ) {
        NavCompassState compassState = base.routeStatus.compassState;
        if (compassState == null || streetOverlay.isEmpty()) {
            return base;
        }
        return new NavState(
                new NavRouteStatus(
                        base.routeStatus.guidance,
                        base.routeStatus.progress,
                        compassState.withStreetOverlay(streetOverlay),
                        base.routeStatus.speedLimit,
                        base.routeStatus.blockedRoadActionAvailable
                ),
                base.gpsStatus,
                base.pauseStatus,
                base.tripStatus
        );
    }

    @NonNull
    public static NavState withPauseState(@NonNull Context context, @NonNull NavState base, boolean paused) {
        return NavStateResourceComposer.withPauseState(new AndroidNavigationTextResources(context), base, paused);
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
    static NavState create(
            @NonNull String nextLine,
            @NonNull String afterNextLine,
            @NonNull String destinationLine,
            @NonNull String stopProgressBlock,
            @NonNull String gpsStatusLine,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull String detailBlock,
            @Nullable NavCompassState compassState,
            boolean paused,
            @NonNull NavGpsTelemetry gpsTelemetry
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
                paused,
                gpsTelemetry
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
        return create(
                nextLine,
                afterNextLine,
                destinationLine,
                stopProgressBlock,
                gpsStatusLine,
                nextEvaluationDeadlineElapsedMs,
                detailBlock,
                compassState,
                speedLimit,
                paused,
                NavGpsTelemetry.unavailable(gpsStatusLine)
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
            boolean paused,
            @NonNull NavGpsTelemetry gpsTelemetry
    ) {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(nextLine, afterNextLine),
                        new NavProgressStatus(destinationLine, stopProgressBlock, detailBlock),
                        compassState,
                        speedLimit
                ),
                new NavGpsStatus(gpsStatusLine, nextEvaluationDeadlineElapsedMs, gpsTelemetry),
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
        return NavStateResourceComposer.buildGpsStatusLine(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount,
                new AndroidNavigationTextResources(context)
        );
    }
}
