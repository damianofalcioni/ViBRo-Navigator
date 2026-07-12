package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.routing.NavigationRouteRecalculationReason;

final class NavigationRerouteFixPath {
    private static final double DUPLICATE_POINT_TOLERANCE_DEGREES = 0.0000001;

    @Nullable
    private LatLon lastStableRouteFix;
    @NonNull
    private final List<LatLon> activeFixes = new ArrayList<>();
    private boolean routeApplied;

    void reset() {
        lastStableRouteFix = null;
        activeFixes.clear();
        routeApplied = false;
    }

    boolean isActive() {
        return !activeFixes.isEmpty();
    }

    void onRouteApplied() {
        if (isActive()) {
            routeApplied = true;
        }
    }

    @NonNull
    List<LatLon> recordEvaluation(
            @NonNull NavigationLocation location,
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        LatLon fix = point(location);
        if (!isActive()) {
            return recordWithoutActivePath(fix, evaluation);
        }

        appendDistinct(activeFixes, fix);
        return routeApplied
                ? recordAfterRouteApplied(fix, evaluation, routeCalculationInProgress)
                : recordBeforeRouteApplied(fix, evaluation, routeCalculationInProgress);
    }

    @NonNull
    private List<LatLon> recordBeforeRouteApplied(
            @NonNull LatLon fix,
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        if (!routeCalculationInProgress && evaluation.isStableOnRouteSample()) {
            discardFailedReroutePath(fix);
        }
        return Collections.emptyList();
    }

    @NonNull
    private List<LatLon> recordAfterRouteApplied(
            @NonNull LatLon fix,
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        if (shouldWaitForFinalRoute(evaluation, routeCalculationInProgress)) {
            return Collections.emptyList();
        }

        List<LatLon> completed = RouteRecalculationBridge.copiedPoints(activeFixes);
        activeFixes.clear();
        routeApplied = false;
        lastStableRouteFix = copy(fix);
        return completed;
    }

    private static boolean shouldWaitForFinalRoute(
            @NonNull NavigationRouteEvaluation evaluation,
            boolean routeCalculationInProgress
    ) {
        return routeCalculationInProgress
                || evaluation.shouldRecalculateRoute()
                || !evaluation.isStableOnRouteSample();
    }

    @NonNull
    private List<LatLon> recordWithoutActivePath(
            @NonNull LatLon fix,
            @NonNull NavigationRouteEvaluation evaluation
    ) {
        if (isRouteDeviationEvaluation(evaluation)) {
            appendDistinct(activeFixes, lastStableRouteFix);
            appendDistinct(activeFixes, fix);
        } else if (evaluation.isStableOnRouteSample()) {
            lastStableRouteFix = copy(fix);
        }
        return Collections.emptyList();
    }

    private void discardFailedReroutePath(@NonNull LatLon fix) {
        activeFixes.clear();
        routeApplied = false;
        lastStableRouteFix = copy(fix);
    }

    private static boolean isRouteDeviationEvaluation(@NonNull NavigationRouteEvaluation evaluation) {
        return evaluation.recalculationReason == NavigationRouteRecalculationReason.ROUTE_DEVIATION;
    }

    private static void appendDistinct(@NonNull List<LatLon> points, @Nullable LatLon point) {
        if (point == null) {
            return;
        }
        if (points.isEmpty() || !samePoint(points.get(points.size() - 1), point)) {
            points.add(copy(point));
        }
    }

    private static boolean samePoint(@NonNull LatLon first, @NonNull LatLon second) {
        return Math.abs(first.lat - second.lat) <= DUPLICATE_POINT_TOLERANCE_DEGREES
                && Math.abs(first.lon - second.lon) <= DUPLICATE_POINT_TOLERANCE_DEGREES;
    }

    @NonNull
    private static LatLon point(@NonNull NavigationLocation location) {
        return new LatLon(location.getLatitude(), location.getLongitude());
    }

    @NonNull
    private static LatLon copy(@NonNull LatLon point) {
        return new LatLon(point.lat, point.lon);
    }
}
