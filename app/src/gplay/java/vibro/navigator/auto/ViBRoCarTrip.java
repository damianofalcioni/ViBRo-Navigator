package vibro.navigator.auto;

import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;

import java.util.TimeZone;

import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavState;

final class ViBRoCarTrip {
    private ViBRoCarTrip() { }

    static Trip from(NavState state) {
        Trip.Builder trip = new Trip.Builder();
        if (state.routeStatus.guidance.maneuvers.isEmpty()) {
            return trip.setLoading(true).build();
        }
        for (NavManeuver maneuver : state.routeStatus.guidance.maneuvers) {
            trip.addStep(ViBRoCarManeuvers.step(maneuver), stepEstimate(maneuver,
                    state.routeStatus.progress.travelEstimate == null ? System.currentTimeMillis()
                            : state.routeStatus.progress.travelEstimate.observationWallTimeMs));
        }
        return trip.build();
    }

    private static TravelEstimate stepEstimate(NavManeuver maneuver, long nowMs) {
        long seconds = Double.isFinite(maneuver.timeSeconds) && maneuver.timeSeconds >= 0
                ? (long) Math.ceil(maneuver.timeSeconds) : TravelEstimate.REMAINING_TIME_UNKNOWN;
        return new TravelEstimate.Builder(ViBRoCarManeuvers.distance(maneuver.distanceMeters),
                DateTimeWithZone.create(nowMs + Math.max(0, seconds) * 1_000L, TimeZone.getDefault()))
                .setRemainingTimeSeconds(seconds).build();
    }
}
