package vibro.navigator.auto;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavState;

/** Labels projected guidance while retaining structured maneuvers and estimates. */
final class ViBRoCarSimulationDisplay {
    interface LabelText { String get(); }

    private final LabelText labelText;
    private NavGuidanceStatus originalGuidance;
    private NavGuidanceStatus markedGuidance;

    ViBRoCarSimulationDisplay(LabelText labelText) {
        this.labelText = labelText;
    }

    NavState label(NavState state) {
        if (state.routeStatus.guidance != originalGuidance) {
            originalGuidance = state.routeStatus.guidance;
            markedGuidance = markGuidance(originalGuidance, labelText.get());
        }
        return new NavState(state.routeStatus.withGuidance(markedGuidance),
                state.gpsStatus, state.pauseStatus, state.tripStatus);
    }

    private static NavGuidanceStatus markGuidance(NavGuidanceStatus guidance, String label) {
        List<NavManeuver> maneuvers = new ArrayList<>(guidance.maneuvers);
        if (!maneuvers.isEmpty()) {
            NavManeuver first = maneuvers.get(0);
            maneuvers.set(0, new NavManeuver(first.hint, label + ": " + first.instruction,
                    first.distanceMeters, first.timeSeconds));
        }
        return new NavGuidanceStatus(label + ": " + guidance.nextLine,
                guidance.afterNextLine, guidance.nextUncertain, guidance.afterNextUncertain, maneuvers);
    }
}
