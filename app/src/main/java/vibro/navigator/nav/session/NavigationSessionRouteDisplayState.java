package vibro.navigator.nav.session;


import vibro.navigator.nav.compass.CompassRadiusTransition;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.routing.NavigationRouteFailureFormatter;
import vibro.navigator.nav.guidance.NavigationRouteProgressTracker;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTarget;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;

import java.util.ArrayList;
import java.util.List;

public final class NavigationSessionRouteDisplayState {

    private static final long NO_COMPASS_RADIUS_UPDATE_TIME_MS = -1L;

    @Nullable
    private CompassRouteGeometry compassRouteGeometry;
    @NonNull
    private List<NavTarget> targets = new ArrayList<>();
    @Nullable
    private Float lastCompassVisibleRadiusMeters;
    @Nullable
    private Float lastReliableMovingCompassVisibleRadiusMeters;
    private float lastSmoothedAccuracyMeters = Float.NaN;
    private long lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
    @NonNull
    private final CompassRadiusTransition compassRadiusTransition = new CompassRadiusTransition(1_000L);

    public void reset() {
        compassRouteGeometry = null;
        targets = new ArrayList<>();
        lastCompassVisibleRadiusMeters = null;
        lastReliableMovingCompassVisibleRadiusMeters = null;
        lastSmoothedAccuracyMeters = Float.NaN;
        lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        compassRadiusTransition.reset();
    }

    public void rememberSmoothedAccuracyMeters(float smoothedAccuracyMeters) {
        lastSmoothedAccuracyMeters = smoothedAccuracyMeters;
    }

    public void onRouteApplied(
            @NonNull Context context,
            @NonNull NavigationRequest request,
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex polylineIndex
    ) {
        compassRouteGeometry = NavState.buildCompassRouteGeometry(route, polylineIndex);
        targets = buildTargets(context, request.stops, polylineIndex);
        lastCompassVisibleRadiusMeters = null;
        lastSmoothedAccuracyMeters = Float.NaN;
        lastCompassRadiusUpdateTimeMs = NO_COMPASS_RADIUS_UPDATE_TIME_MS;
        compassRadiusTransition.reset();
    }

    @NonNull
    public NavState buildState(
            @NonNull Context context,
            @Nullable GeoJsonRoute route,
            @Nullable PolylineIndex polylineIndex,
            int lastSegmentIndex,
            @NonNull NavigationTurnState turnState,
            @NonNull NavigationRouteProgressTracker progressTracker,
            @Nullable Location lastFiltered,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            boolean routeCalculationInProgress,
            @Nullable String routeCalculationNotice,
            @Nullable Throwable lastRouteFailure
    ) {
        String gpsStatusLine = buildGpsStatusLine(context, lastFiltered, speedMps, accuracyMeters, fixedSatelliteCount);
        if (lastFiltered == null) {
            return buildStateWithoutLocation(context, nextEvaluationDeadlineElapsedMs, lastRouteFailure, gpsStatusLine);
        }

        if (routeCalculationInProgress) {
            return buildCalculatingState(context, nextEvaluationDeadlineElapsedMs, routeCalculationNotice, gpsStatusLine);
        }

        if (route == null || polylineIndex == null) {
            return buildStateWithoutRoute(context, nextEvaluationDeadlineElapsedMs, lastRouteFailure, gpsStatusLine);
        }

        PolylineIndex.Match match = polylineIndex.match(
                new LatLon(lastFiltered.getLatitude(), lastFiltered.getLongitude()),
                lastSegmentIndex
        );
        if (match == null) {
            return NavState.withGpsStatus(NavState.waiting(context), gpsStatusLine);
        }

        float etaSpeedMps = progressTracker.resolveEtaSpeedMps(
                lastFiltered,
                match.alongTrackMeters,
                accuracyMeters,
                likelyStationary
        );
        NavState state = NavState.from(
                route,
                polylineIndex,
                match.alongTrackMeters,
                turnState.getNextHintIdx(),
                match.segmentIndex,
                speedMps,
                etaSpeedMps,
                likelyStationary,
                accuracyMeters,
                resolveCompassAccuracyMeters(accuracyMeters),
                lastFiltered,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                lastCompassVisibleRadiusMeters,
                lastReliableMovingCompassVisibleRadiusMeters,
                resolveCompassRadiusUpdateDeltaMs(nowMs),
                compassRouteGeometry,
                compassRadiusTransition,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                turnState.isDestinationReached(),
                targets,
                context
        );
        rememberCompassState(state, nowMs, lastFiltered, likelyStationary);
        return withLastRouteFailureNotice(context, state, lastRouteFailure);
    }

    @NonNull
    private String buildGpsStatusLine(
            @NonNull Context context,
            @Nullable Location lastFiltered,
            float speedMps,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (lastFiltered == null) {
            return NavState.buildGpsStatusLine(Float.NaN, null, Float.NaN, fixedSatelliteCount, context);
        }
        return NavState.buildGpsStatusLine(speedMps, lastFiltered, accuracyMeters, fixedSatelliteCount, context);
    }

    @NonNull
    private NavState buildStateWithoutLocation(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable Throwable lastRouteFailure,
            @NonNull String gpsStatusLine
    ) {
        if (lastRouteFailure != null) {
            return NavState.withGpsStatus(NavState.routeUnavailable(
                    context,
                    NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                    nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavState.withGpsStatus(
                NavState.waitingForLocation(context, nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState buildCalculatingState(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable String routeCalculationNotice,
            @NonNull String gpsStatusLine
    ) {
        NavState calculatingState = NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs);
        if (routeCalculationNotice != null && !routeCalculationNotice.trim().isEmpty()) {
            calculatingState = NavState.withNotice(calculatingState, routeCalculationNotice);
        }
        return NavState.withGpsStatus(calculatingState, gpsStatusLine);
    }

    @NonNull
    private NavState buildStateWithoutRoute(
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @Nullable Throwable lastRouteFailure,
            @NonNull String gpsStatusLine
    ) {
        if (lastRouteFailure != null) {
            return NavState.withGpsStatus(NavState.routeUnavailable(
                    context,
                    NavigationRouteFailureFormatter.format(context, lastRouteFailure, false),
                    nextEvaluationDeadlineElapsedMs
            ), gpsStatusLine);
        }
        return NavState.withGpsStatus(
                NavState.calculatingRoute(context, nextEvaluationDeadlineElapsedMs),
                gpsStatusLine
        );
    }

    @NonNull
    private NavState withLastRouteFailureNotice(
            @NonNull Context context,
            @NonNull NavState state,
            @Nullable Throwable lastRouteFailure
    ) {
        return lastRouteFailure != null
                ? NavState.withNotice(state, NavigationRouteFailureFormatter.format(context, lastRouteFailure, true))
                : state;
    }

    private float resolveCompassAccuracyMeters(float fallbackAccuracyMeters) {
        if (Float.isFinite(lastSmoothedAccuracyMeters) && lastSmoothedAccuracyMeters > 0f) {
            return lastSmoothedAccuracyMeters;
        }
        return Float.isFinite(fallbackAccuracyMeters) && fallbackAccuracyMeters > 0f
                ? fallbackAccuracyMeters
                : 0f;
    }

    @NonNull
    private List<NavTarget> buildTargets(
            @NonNull Context context,
            @NonNull List<LatLon> intermediates,
            @NonNull PolylineIndex index
    ) {
        List<NavTarget> out = new ArrayList<>();
        for (int i = 0; i < intermediates.size(); i++) {
            PolylineIndex.Match match = index.match(intermediates.get(i), -1);
            if (match != null) {
                out.add(new NavTarget(context.getString(R.string.format_stop_label, i + 1), match.alongTrackMeters));
            }
        }
        out.add(new NavTarget(context.getString(R.string.nav_destination_label), index.totalLengthMeters()));
        return out;
    }

    private long resolveCompassRadiusUpdateDeltaMs(long nowMs) {
        if (lastCompassRadiusUpdateTimeMs == NO_COMPASS_RADIUS_UPDATE_TIME_MS || nowMs <= lastCompassRadiusUpdateTimeMs) {
            return 0L;
        }
        return nowMs - lastCompassRadiusUpdateTimeMs;
    }

    private void rememberCompassState(
            @NonNull NavState state,
            long nowMs,
            @Nullable Location lastFiltered,
            boolean likelyStationary
    ) {
        if (state.routeStatus.compassState == null) {
            return;
        }
        lastCompassVisibleRadiusMeters = state.routeStatus.compassState.radiusState.visibleRadiusMeters;
        lastCompassRadiusUpdateTimeMs = nowMs;
        if (lastFiltered != null && NavState.hasReliableMovingSpeed(lastFiltered, likelyStationary)) {
            lastReliableMovingCompassVisibleRadiusMeters = state.routeStatus.compassState.radiusState.visibleRadiusMeters;
        }
    }
}
