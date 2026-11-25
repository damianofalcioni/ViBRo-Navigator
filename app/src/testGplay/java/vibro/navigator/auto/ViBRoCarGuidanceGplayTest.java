package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.car.app.CarContext;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Trip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavManeuver;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavStopEstimate;
import vibro.navigator.nav.model.NavTravelEstimate;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.route.RouteStartApproach;
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
        assertNotNull(routing(metric).getCurrentStep().getManeuver().getIcon());
        assertEquals(Distance.create(2.5, Distance.UNIT_KILOMETERS_P1),
                metric.getDestinationTravelEstimate().getRemainingDistance());

        AppSettings.setImperialUnitsEnabled(context, true);
        NavigationTemplate imperial = template(state);
        Distance feet = Distance.create(394, Distance.UNIT_FEET);
        assertEquals(feet, routing(imperial).getCurrentDistance());
        assertEquals(Distance.create(1.5, Distance.UNIT_MILES_P1),
                imperial.getDestinationTravelEstimate().getRemainingDistance());
        Trip trip = ViBRoCarTrip.from(state, true, ViBRoCarManeuvers.Icons.from(context));
        assertEquals(feet, trip.getStepTravelEstimates().get(0).getRemainingDistance());
        assertNotNull(trip.getSteps().get(0).getManeuver().getIcon());
    }

    @Test
    public void retainedRouteFailureKeepsCueAndShowsWarning() {
        String warning = "No detour found. Keeping current route.";
        NavState state = NavStateComposer.withNotice(base, warning);
        NavigationTemplate car = template(state);

        RoutingInfo info = routing(car);
        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, info.getCurrentStep().getManeuver().getType());
        assertEquals(Distance.create(120, Distance.UNIT_METERS), info.getCurrentDistance());
        assertTrue(info.getCurrentStep().getCue().toString().contains(warning));
        assertEquals("Turn right", ViBRoCarTrip.from(state, false, null).getSteps().get(0).getCue().toString());
    }

    @Test
    public void unknownArrivalShowsKnownDestinationDistanceWithoutInventingTime() {
        NavState state = withTravelEstimate(500, Double.NaN);
        NavigationTemplate car = template(state);

        assertNull(car.getDestinationTravelEstimate());
        assertTrue(routing(car).getCurrentStep().getCue().toString().contains("500 m"));
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
        Trip trip = ViBRoCarTrip.from(state, false, ViBRoCarManeuvers.Icons.from(context));
        assertEquals("≈ Turn right · 12 s", info.getCurrentStep().getCue().toString());
        assertEquals("≈ Turn left · 20 m · 3 s", info.getNextStep().getCue().toString());
        assertEquals(info.getCurrentStep().getManeuver().getType(), trip.getSteps().get(0).getManeuver().getType());
        assertNotNull(info.getNextStep().getManeuver().getIcon());
        assertNotNull(trip.getSteps().get(0).getManeuver().getIcon());
        assertEquals("≈ Turn right", trip.getSteps().get(0).getCue().toString());
        assertEquals("≈ Turn left", trip.getSteps().get(1).getCue().toString());
        assertEquals(Distance.create(140, Distance.UNIT_METERS),
                trip.getStepTravelEstimates().get(1).getRemainingDistance());
    }

    @Test
    public void stopProgressAndFinalDestinationEstimateCoexist() {
        NavStopEstimate stop = new NavStopEstimate("Stop 2", 350, 90.0);
        NavState state = withProgress(new NavTravelEstimate(1_200, 140, 1_700_000_000_000L, false), stop);
        NavigationTemplate car = template(state);
        String cue = routing(car).getCurrentStep().getCue().toString();
        assertTrue(cue.contains(stop.label()));
        assertTrue(cue.contains("350 m"));
        assertTrue(cue.contains(NavigationTextFormatter.formatEta(1_700_000_090_000L)));
        assertEquals(140, car.getDestinationTravelEstimate().getRemainingTimeSeconds());
    }

    @Test
    public void templateRefreshesWhenAnArrivalMinuteChangesFromTheLastPublishedView() {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(context, () -> { });
        NavStopEstimate stop = new NavStopEstimate("Stop 1", 350, 90.0);
        long start = 1_700_000_040_000L;
        NavState first = withProgress(new NavTravelEstimate(500, 90, start, false), stop);
        NavState shortlyAfter = withProgress(new NavTravelEstimate(500, 90, start + 10_000, false), stop);
        NavState updated = withProgress(new NavTravelEstimate(500, 90, start + 120_000, false), stop);
        NavState nextFix = withProgress(new NavTravelEstimate(500, 90, start + 121_000, false), stop);
        templates.build(first);
        assertFalse(templates.requiresInvalidation(first, shortlyAfter));
        assertTrue(templates.requiresInvalidation(updated, nextFix));

        templates.build(nextFix);
        assertFalse(templates.requiresInvalidation(updated, nextFix));
        AppSettings.setImperialUnitsEnabled(context, true);
        assertTrue(templates.requiresInvalidation(nextFix, nextFix));
    }

    @Test
    public void directGuidanceShowsAnIconApproachTimeAndTheFollowingRoadCue() {
        var direct = new NavManeuver(new VoiceHint(0, RouteStartApproach.BEELINE_COMMAND, 0, 0, 0), "Beeline", 95, 40);
        var guidance = new NavGuidanceStatus("Beeline", "Turn left after joining the road").withManeuvers(List.of(direct));
        var state = withGuidance(guidance);
        RoutingInfo info = routing(template(state));

        assertEquals("Beeline · 40 s", info.getCurrentStep().getCue().toString());
        assertEquals(Distance.create(95, Distance.UNIT_METERS), info.getCurrentDistance());
        assertEquals(guidance.afterNextLine, info.getNextStep().getCue().toString());
        var icons = ViBRoCarManeuvers.Icons.from(context);
        var maneuver = info.getCurrentStep().getManeuver();
        assertEquals(Maneuver.TYPE_UNKNOWN, maneuver.getType());
        assertEquals(icons.beeline(), maneuver.getIcon());
        Trip trip = ViBRoCarTrip.from(state, false, icons);
        assertEquals(1, trip.getSteps().size());
        assertEquals(maneuver, trip.getSteps().get(0).getManeuver());
    }

    @Test
    public void uTurnWithoutASideGetsAnIconAndAnUnavailableDurationStaysHidden() {
        var uTurn = new NavManeuver(new VoiceHint(0, 15, 0, 0, 180), "U-turn", 95, Double.NaN);
        var state = withGuidance(new NavGuidanceStatus("U-turn", "").withManeuvers(List.of(uTurn)));
        var step = routing(template(state)).getCurrentStep();
        assertEquals("U-turn", step.getCue().toString());
        assertEquals(Maneuver.TYPE_UNKNOWN, step.getManeuver().getType());
        assertNotNull(step.getManeuver().getIcon());
        assertEquals(step.getManeuver(), ViBRoCarTrip.from(state, false, ViBRoCarManeuvers.Icons.from(context))
                .getSteps().get(0).getManeuver());
    }

    @Test
    public void confirmedArrivalClearsEtaAndIgnoresLaterObservationTimes() {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(context, () -> { });
        long start = 1_700_000_040_000L;
        NavState arrived = arrivedAt(start);
        assertTrue(templates.requiresInvalidation(base, arrived));
        NavigationTemplate arrival = (NavigationTemplate) templates.build(arrived);
        assertNull(arrival.getDestinationTravelEstimate());
        MessageInfo message = (MessageInfo) arrival.getNavigationInfo();
        assertEquals(context.getString(R.string.nav_destination_reached), message.getTitle().toString());
        assertFalse(templates.requiresInvalidation(arrived, arrivedAt(start + 120_000)));
    }

    private NavState arrivedAt(long observationTimeMs) {
        String reached = context.getString(R.string.nav_destination_reached);
        return new NavState(base.routeStatus.withGuidance(new NavGuidanceStatus(reached, ""))
                .withProgress(new NavProgressStatus(reached, "", "",
                        new NavTravelEstimate(0, 0, observationTimeMs, true))), base.gpsStatus, base.pauseStatus);
    }

    private NavState withGuidance(NavGuidanceStatus guidance) {
        return new NavState(base.routeStatus.withGuidance(guidance), base.gpsStatus, base.pauseStatus, base.tripStatus);
    }

    private NavState withTravelEstimate(double meters, double seconds) {
        return withProgress(new NavTravelEstimate(meters, seconds, 1_700_000_000_000L, false), null);
    }

    private NavState withProgress(NavTravelEstimate estimate, NavStopEstimate stop) {
        NavRouteStatus route = base.routeStatus.withProgress(new NavProgressStatus("Destination", "", "", estimate, stop));
        return new NavState(route, base.gpsStatus, base.pauseStatus, base.tripStatus);
    }

    private NavigationTemplate template(NavState state) {
        return (NavigationTemplate) new ViBRoCarTemplates(context, () -> { }).build(state);
    }

    private static RoutingInfo routing(NavigationTemplate template) {
        return (RoutingInfo) template.getNavigationInfo();
    }
}
