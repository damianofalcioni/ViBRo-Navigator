package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavStateTextFactory;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavStopEstimate;

final class StraightLineNavigationProgressText {
    private StraightLineNavigationProgressText() {
    }

    @NonNull
    static String buildDestinationLine(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot,
            boolean destinationReached,
            int nextStopIndex
    ) {
        if (destinationReached) {
            return snapshot.textResources.getString(R.string.nav_destination_reached);
        }
        if (request.destination == null || snapshot.lastFiltered == null) {
            return "";
        }
        double distanceMeters = StraightLineNavigationProgress.remainingDistanceToDestination(
                request,
                snapshot.lastFiltered,
                nextStopIndex
        );
        return NavStateTextFactory.buildProgressLine(
                snapshot.textResources,
                snapshot.textResources.getString(R.string.nav_destination_label),
                distanceMeters,
                StraightLineNavigationProgress.estimateSeconds(
                        distanceMeters,
                        snapshot.speedMps,
                        snapshot.likelyStationary
                ),
                snapshot.nowMs
        );
    }

    @Nullable
    static NavStopEstimate nextStopEstimate(
            @NonNull NavigationRequest request,
            @NonNull NavigationDisplaySnapshot snapshot,
            boolean destinationReached,
            int nextStopIndex
    ) {
        if (destinationReached || nextStopIndex >= request.stops.size() || snapshot.lastFiltered == null) {
            return null;
        }
        LatLon stop = request.stops.get(nextStopIndex);
        double distanceMeters = StraightLineNavigationProgress.distanceMeters(snapshot.lastFiltered, stop);
        return new NavStopEstimate(
                snapshot.textResources.getString(R.string.format_stop_label, nextStopIndex + 1),
                distanceMeters,
                StraightLineNavigationProgress.estimateSeconds(
                        distanceMeters,
                        snapshot.speedMps,
                        snapshot.likelyStationary
                )
        );
    }
}
