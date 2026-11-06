package vibro.navigator.nav.simulation;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavState;

/** Label every navigation surface while retaining machine-readable maneuvers and estimates. */
public final class NavigationSimulationDisplay {
    private NavigationSimulationDisplay() { }

    public static NavState label(NavState state, String label) {
        NavGuidanceStatus guidance = state.routeStatus.guidance;
        List<NavManeuver> maneuvers = new ArrayList<>(guidance.maneuvers);
        if (!maneuvers.isEmpty()) {
            NavManeuver first = maneuvers.get(0);
            maneuvers.set(0, new NavManeuver(first.hint, label + ": " + first.instruction,
                    first.distanceMeters, first.timeSeconds));
        }
        NavGuidanceStatus marked = new NavGuidanceStatus(label + ": " + guidance.nextLine,
                guidance.afterNextLine, guidance.nextUncertain, guidance.afterNextUncertain, maneuvers);
        return new NavState(state.routeStatus.withGuidance(marked),
                state.gpsStatus.withStatusLine(label + " · " + state.gpsStatus.statusLine),
                state.pauseStatus, state.tripStatus);
    }
}
