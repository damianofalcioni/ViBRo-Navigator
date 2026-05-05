package vibro.navigator.nav.model;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.compass.NavCompassStateInput;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

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
        return from(route, index, alongTrackMeters, nextHintIdx, currentSegmentIndex, speedMps, speedMps,
                likelyStationary, accuracyMeters, accuracyMeters, currentLocation, fixedSatelliteCount,
                headingDegrees, headingAccuracyDegrees, previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters, compassRadiusUpdateDeltaMs, null, null,
                nextEvaluationDeadlineElapsedMs, nowMs, false, targets, context);
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
        return from(route, index, alongTrackMeters, nextHintIdx, currentSegmentIndex, speedMps, etaSpeedMps,
                likelyStationary, accuracyMeters, accuracyMeters, currentLocation, fixedSatelliteCount,
                headingDegrees, headingAccuracyDegrees, previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters, compassRadiusUpdateDeltaMs, null, null,
                nextEvaluationDeadlineElapsedMs, nowMs, false, targets, context);
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
        return from(NavStateBuildInput.builder(context, route, index, currentLocation)
                .routeProgress(alongTrackMeters, nextHintIdx, currentSegmentIndex)
                .motion(speedMps, etaSpeedMps, likelyStationary, accuracyMeters, compassAccuracyMeters)
                .gps(fixedSatelliteCount)
                .heading(headingDegrees, headingAccuracyDegrees)
                .compassMemory(
                        previousCompassVisibleRadiusMeters,
                        previousReliableMovingCompassVisibleRadiusMeters,
                        compassRadiusUpdateDeltaMs
                )
                .compassGeometry(compassRouteGeometry, compassRadiusTransition)
                .timing(nextEvaluationDeadlineElapsedMs, nowMs)
                .destinationReached(destinationReached)
                .targets(targets)
                .build());
    }

    @NonNull
    public static NavState from(@NonNull NavStateBuildInput input) {
        List<String> directionLines = NavStateTextFactory.buildDirectionLines(
                input.route,
                input.index,
                input.alongTrackMeters,
                input.nextHintIndex,
                input.currentSegmentIndex,
                input.etaSpeedMps,
                input.accuracyMeters,
                input.destinationReached,
                input.context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
        String destination = NavStateTextFactory.buildDestinationLine(
                input.route,
                input.index,
                input.alongTrackMeters,
                input.currentSegmentIndex,
                input.etaSpeedMps,
                input.nowMs,
                input.destinationReached,
                input.targets,
                input.context
        );
        String stopProgress = NavStateTextFactory.buildStopProgress(
                input.route,
                input.index,
                input.alongTrackMeters,
                input.currentSegmentIndex,
                input.etaSpeedMps,
                input.nowMs,
                input.destinationReached,
                input.targets,
                input.context
        );
        String gpsStatus = buildGpsStatusLine(
                input.speedMps,
                input.currentLocation,
                input.accuracyMeters,
                input.fixedSatelliteCount,
                input.context
        );
        NavCompassState compassState = NavCompassStateFactory.buildCompassState(compassInput(input));
        return create(
                next,
                afterNext,
                destination,
                stopProgress,
                gpsStatus,
                input.nextEvaluationDeadlineElapsedMs,
                "",
                compassState,
                false
        );
    }

    @NonNull
    private static NavCompassStateInput compassInput(@NonNull NavStateBuildInput input) {
        return NavCompassStateInput.builder(input.route, input.index, input.currentLocation)
                .routeProgress(input.alongTrackMeters)
                .motion(input.speedMps, input.likelyStationary, input.compassAccuracyMeters)
                .heading(input.headingDegrees, input.headingAccuracyDegrees)
                .radiusMemory(
                        input.previousCompassVisibleRadiusMeters,
                        input.previousReliableMovingCompassVisibleRadiusMeters,
                        input.compassRadiusUpdateDeltaMs
                )
                .geometry(input.compassRouteGeometry, input.compassRadiusTransition)
                .nowMs(input.nowMs)
                .build();
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
}
