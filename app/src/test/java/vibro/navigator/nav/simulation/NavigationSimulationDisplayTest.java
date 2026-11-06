package vibro.navigator.nav.simulation;

import org.junit.Test;
import java.util.Collections;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.VoiceHint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NavigationSimulationDisplayTest {
    private static final String LABEL = "Simulation";
    @Test
    public void phoneAndExternalGuidanceAreMarkedWithoutLosingMetadata() {
        NavManeuver maneuver = new NavManeuver(new VoiceHint(1, 2, 0, 10, 5), "Turn", 120, 25);
        NavGuidanceStatus guidance = new NavGuidanceStatus("Turn", "Then", false, false,
                Collections.singletonList(maneuver));
        NavState original = new NavState(new NavRouteStatus(guidance, new NavProgressStatus("End", "", ""), null),
                new NavGpsStatus("GPS", -1), new NavPauseStatus(false));
        NavState marked = NavigationSimulationDisplay.label(original, LABEL);
        assertTrue(marked.routeStatus.guidance.nextLine.startsWith(LABEL));
        assertTrue(marked.routeStatus.guidance.maneuvers.get(0).instruction.startsWith(LABEL));
        assertTrue(marked.gpsStatus.statusLine.startsWith(LABEL));
        assertEquals(120, marked.routeStatus.guidance.maneuvers.get(0).distanceMeters, 0);
        assertSame(maneuver.hint, marked.routeStatus.guidance.maneuvers.get(0).hint);
        assertSame(original.routeStatus.progress, marked.routeStatus.progress);
        assertEquals("Turn", original.routeStatus.guidance.nextLine);
    }
}
