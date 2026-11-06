package vibro.navigator.auto;

import java.util.Collections;

import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;
import vibro.navigator.nav.route.VoiceHint;

final class CarNavigationTestStates {
    private CarNavigationTestStates() { }

    static NavState active(boolean paused, boolean arrived) {
        return new NavState(new NavRouteStatus(
                new NavGuidanceStatus("Turn right in 120 m", "", false, false,
                        Collections.singletonList(new NavManeuver(new VoiceHint(3, 5, 0, 0, 90),
                                "Turn right", 120, 12))),
                new NavProgressStatus("Destination", "", "",
                        new NavTravelEstimate(arrived ? 0 : 500, arrived ? 0 : 50, 1_700_000_000_000L, arrived)),
                null, null, true), new NavGpsStatus("", NavState.NO_DEADLINE), new NavPauseStatus(paused));
    }

    static NavState loading() {
        return new NavState(new NavRouteStatus(new NavGuidanceStatus("Calculating route", ""),
                new NavProgressStatus("", "", ""), null), new NavGpsStatus("", NavState.NO_DEADLINE),
                new NavPauseStatus(false));
    }
}
