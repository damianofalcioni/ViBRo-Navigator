package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.car.app.CarContext;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Trip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTravelEstimate;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarGuidanceGplayTest {
    private final NavState base = CarNavigationTestStates.active(false, false);
    private CarContext context;

    @Before
    public void setUp() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        context = contexts.testCarContext(1);
    }

    @Test
    public void maneuverDestinationAndClusterDistancesFollowMeasurementPreference() {
        NavState state = withTravelEstimate(2_450, 400);
        NavigationTemplate metric = template(state);
        assertEquals(Distance.create(120, Distance.UNIT_METERS), routing(metric).getCurrentDistance());
        assertEquals(Distance.create(2.5, Distance.UNIT_KILOMETERS_P1),
                metric.getDestinationTravelEstimate().getRemainingDistance());

        AppSettings.setImperialUnitsEnabled(context, true);
        NavigationTemplate imperial = template(state);
        Distance feet = Distance.create(394, Distance.UNIT_FEET);
        assertEquals(feet, routing(imperial).getCurrentDistance());
        assertEquals(Distance.create(1.5, Distance.UNIT_MILES_P1),
                imperial.getDestinationTravelEstimate().getRemainingDistance());
        assertEquals(feet, ViBRoCarTrip.from(state, true).getStepTravelEstimates().get(0).getRemainingDistance());
    }

    @Test
    public void retainedRouteFailureKeepsCueAndShowsWarning() {
        String warning = "No detour found. Keeping current route.";
        NavState state = NavStateComposer.withNotice(base, warning);
        NavigationTemplate car = template(state);

        MessageInfo message = (MessageInfo) car.getNavigationInfo();
        assertEquals(base.routeStatus.guidance.nextLine, message.getTitle().toString());
        assertEquals(warning, message.getText().toString());
        assertEquals("Turn right", ViBRoCarTrip.from(state, false).getSteps().get(0).getCue().toString());
    }

    @Test
    public void unknownArrivalShowsKnownDestinationDistanceWithoutInventingTime() {
        NavState state = withTravelEstimate(500, Double.NaN);
        NavigationTemplate car = template(state);

        assertNull(car.getDestinationTravelEstimate());
        MessageInfo message = (MessageInfo) car.getNavigationInfo();
        assertEquals(base.routeStatus.guidance.nextLine, message.getTitle().toString());
        assertTrue(message.getText().toString().contains("500 m"));
    }

    @Test
    public void uncertainManeuversAreMarkedInTemplateAndClusterWithoutChangingOrder() {
        NavManeuver current = base.routeStatus.guidance.maneuvers.get(0);
        NavManeuver next = new NavManeuver(new VoiceHint(4, 2, 0, 0, 0), "Turn left", 140, 15);
        NavGuidanceStatus guidance = new NavGuidanceStatus(current.instruction, next.instruction, true, true,
                Arrays.asList(current, next));
        NavState state = new NavState(base.routeStatus.withGuidance(guidance), base.gpsStatus,
                base.pauseStatus, base.tripStatus);

        RoutingInfo info = routing(template(state));
        Trip trip = ViBRoCarTrip.from(state, false);
        assertEquals("≈ Turn right", info.getCurrentStep().getCue().toString());
        assertEquals("≈ Turn left", info.getNextStep().getCue().toString());
        assertEquals(info.getCurrentStep(), trip.getSteps().get(0));
        assertEquals(info.getNextStep(), trip.getSteps().get(1));
    }

    private NavState withTravelEstimate(double meters, double seconds) {
        NavRouteStatus route = base.routeStatus.withProgress(new NavProgressStatus("Destination", "", "",
                new NavTravelEstimate(meters, seconds, 1_700_000_000_000L, false)));
        return new NavState(route, base.gpsStatus, base.pauseStatus, base.tripStatus);
    }

    private NavigationTemplate template(NavState state) {
        return (NavigationTemplate) new ViBRoCarTemplates(context, () -> { }).build(state);
    }

    private static RoutingInfo routing(NavigationTemplate template) {
        return (RoutingInfo) template.getNavigationInfo();
    }
}
