package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateResourceComposer;
import vibro.navigator.nav.routing.NavigationRouteFailureFormatter;
import vibro.navigator.nav.startup.NavigationStartupLocationSelector;

final class NavigationNoRouteDisplayState {

    private NavigationNoRouteDisplayState() {
    }

    @NonNull
    static NavState build(@NonNull NavigationDisplaySnapshot snapshot) {
        if (snapshot.lastRouteFailure != null) {
            return NavigationDisplayGpsStatusFactory.withSnapshotGpsStatus(NavStateResourceComposer.routeUnavailable(
                    snapshot.textResources,
                    NavigationRouteFailureFormatter.format(snapshot.textResources, snapshot.lastRouteFailure, false),
                    snapshot.nextEvaluationDeadlineElapsedMs
            ), snapshot);
        }
        if (!isRouteStartAccuracyUsable(snapshot.accuracyMeters)) {
            return NavigationDisplayGpsStatusFactory.withSnapshotGpsStatus(
                    NavStateResourceComposer.waitingForLocation(
                            snapshot.textResources,
                            snapshot.nextEvaluationDeadlineElapsedMs
                    ),
                    snapshot
            );
        }
        return NavigationDisplayGpsStatusFactory.withSnapshotGpsStatus(
                NavStateResourceComposer.calculatingRoute(
                        snapshot.textResources,
                        snapshot.nextEvaluationDeadlineElapsedMs
                ),
                snapshot
        );
    }

    private static boolean isRouteStartAccuracyUsable(float accuracyMeters) {
        return Float.isFinite(accuracyMeters)
                && accuracyMeters > 0f
                && accuracyMeters <= NavigationStartupLocationSelector.MAX_ROUTE_START_ACCURACY_METERS;
    }
}
