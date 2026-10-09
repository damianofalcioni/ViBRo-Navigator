package vibro.navigator.nav.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnState;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

public class UpcomingManeuverAccuracyTest {
    private static final float WALKING_SPEED_MPS = 1.45f;

    @Test
    public void loggedContinueAlertsMatchTheVisibleOrangeInstruction() {
        Fixture fixture = new Fixture(1);

        Observation preparatory = fixture.observe(28.1175, WALKING_SPEED_MPS, 30.328f, false, true);
        preparatory.assertCurrentAlert();
        preparatory.assertUncertain();
        assertFalse(preparatory.guidance.afterNextUncertain);
        assertTrue(preparatory.guidance.afterNextLine.contains("Turn left"));
        fixture.observe(6.9216, WALKING_SPEED_MPS, 39.317f, false, true).assertCurrentAlert();
        assertEquals(0, fixture.state.getNextHintIdx());
    }

    @Test
    public void accuracyRecoveryChangesOnlyColorWithoutRepeatingSentAlerts() {
        Fixture fixture = new Fixture(1);

        Observation coarse = fixture.observe(28.0, WALKING_SPEED_MPS, 30f, false, true);
        coarse.assertUncertain();
        coarse.assertCurrentAlert();
        Observation accurate = fixture.observe(28.0, WALKING_SPEED_MPS, 4f, false, true);
        assertEquals(coarse.guidance.nextLine, accurate.guidance.nextLine);
        assertFalse(accurate.guidance.nextUncertain);
        assertTrue(accurate.events.isEmpty());
        fixture.observe(7.0, WALKING_SPEED_MPS, 4f, false, true).assertCurrentAlert();
        assertTrue(fixture.observe(7.0, WALKING_SPEED_MPS, 4f, false, true).events.isEmpty());
    }

    @Test
    public void unstableProgressKeepsAlertsPendingAndRecoveryEmitsOnlyMostUrgentAlert() {
        Fixture fixture = new Fixture(1);

        assertTrue(fixture.observe(28.0, WALKING_SPEED_MPS, 30f, false, false).events.isEmpty());
        Observation held = fixture.observe(7.0, WALKING_SPEED_MPS, 39f, false, false);
        held.assertUncertain();
        assertTrue(held.events.isEmpty());
        Observation recovered = fixture.observe(7.0, WALKING_SPEED_MPS, 4f, false, true);
        recovered.assertCurrentAlert();
        assertTrue(recovered.events.get(0).timeSeconds <= 5.0);
        assertTrue(fixture.observe(7.0, WALKING_SPEED_MPS, 4f, false, true).events.isEmpty());
    }

    @Test
    public void unstableProjectionBeyondTurnDoesNotAdvanceGuidance() {
        Fixture fixture = new Fixture(1);

        Observation held = fixture.observe(-6.0, WALKING_SPEED_MPS, 39f, false, false);
        held.assertUncertain();
        assertTrue(held.events.isEmpty());
        assertEquals(0, fixture.state.getNextHintIdx());
        fixture.observe(7.0, WALKING_SPEED_MPS, 4f, false, true).assertCurrentAlert();
    }

    @Test
    public void accuracyFluctuationDoesNotHideOrAdvanceCurrentManeuver() {
        Fixture fixture = new Fixture(1);

        Observation accurate = fixture.observe(28.0, WALKING_SPEED_MPS, 4f, false, true);
        Observation coarse = fixture.observe(28.0, WALKING_SPEED_MPS, 150f, false, true);
        coarse.assertUncertain();
        assertEquals(accurate.guidance.nextLine, coarse.guidance.nextLine);
        assertTrue(coarse.guidance.afterNextUncertain);
        assertTrue(coarse.events.isEmpty());
        assertEquals(0, fixture.state.getNextHintIdx());
    }

    @Test
    public void passedManeuverDoesNotReappearWhenAccuracyRecovers() {
        Fixture fixture = new Fixture(1);
        fixture.observe(7.0, WALKING_SPEED_MPS, 39f, false, true).assertCurrentAlert();

        Observation passed = fixture.observe(-6.0, WALKING_SPEED_MPS, 4f, false, true);
        assertEquals(1, fixture.state.getNextHintIdx());
        assertEquals(2, passed.upcoming.get(0).hint.indexInTrack);
        assertFalse(passed.guidance.nextUncertain);
        assertTrue(fixture.observe(-7.0, WALKING_SPEED_MPS, 1f, false, true).events.isEmpty());
    }

    @Test
    public void exactAccuracyBoundaryIsOrangeUntilDistanceExceedsUncertainty() {
        Fixture fixture = new Fixture(1);
        fixture.observe(7.0, WALKING_SPEED_MPS, 7f, false, true).assertUncertain();
        assertFalse(fixture.observe(7.0, WALKING_SPEED_MPS, 6.9f, false, true).guidance.nextUncertain);
    }

    @Test
    public void missingAccuracyKeepsTextVisibleAndMarksItUncertain() {
        for (float accuracy : new float[]{Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, -1f}) {
            Fixture fixture = new Fixture(1);
            Observation observation = fixture.observe(28.0, WALKING_SPEED_MPS, accuracy, false, false);
            observation.assertUncertain();
            assertTrue(observation.events.isEmpty());
        }
    }

    @Test
    public void singleInstructionModeAlertsForVisibleOrangeManeuverOnce() {
        Fixture fixture = new Fixture(1);
        fixture.observe(14.0, WALKING_SPEED_MPS, 20f, true, true).assertCurrentAlert();
        assertTrue(fixture.observe(7.0, WALKING_SPEED_MPS, 4f, true, true).events.isEmpty());
    }

    @Test
    public void slowWalkingKeepsFinalInstructionVisibleEvenWhenTooLateToAlert() {
        Fixture fixture = new Fixture(1);
        fixture.observe(2.5, 0.5f, 3f, false, true).assertCurrentAlert();
        Observation veryClose = fixture.observe(0.8, 0.5f, 1f, false, true);
        veryClose.assertUncertain();
        assertTrue(veryClose.events.isEmpty());
        assertEquals(0, fixture.state.getNextHintIdx());
    }

    @Test
    public void arrivalInstructionsRemainVisibleAndUseTheirReachedPolicy() {
        for (int command : new int[]{100, 101}) {
            Fixture fixture = new Fixture(command);
            fixture.observe(7.0, WALKING_SPEED_MPS, 39f, false, true).assertCurrentAlert();
        }
    }

    @Test
    public void reachedBeelineStartDoesNotDuplicateActiveDirectGuidance() {
        Fixture fixture = new Fixture(RouteStartApproach.BEELINE_COMMAND);
        Observation beforeStart = fixture.observe(1.0, WALKING_SPEED_MPS, 39f, false, false);
        assertEquals(RouteStartApproach.BEELINE_COMMAND, beforeStart.upcoming.get(0).hint.command);
        Observation atStart = fixture.observe(0.0, WALKING_SPEED_MPS, 39f, false, false);
        assertEquals(2, atStart.upcoming.get(0).hint.command);
        assertTrue(atStart.guidance.nextLine.contains("Turn left"));
        assertFalse(atStart.guidance.nextUncertain);
    }

    @Test
    public void initialAlertWaitsForAccuracyButTheManeuverRemainsVisible() {
        Fixture fixture = new Fixture(1);
        LatLon position = new LatLon(0.0, 0.00075);
        assertTrue(fixture.state.onRouteApplied(fixture.route, fixture.index,
                Collections.emptyList(), position, WALKING_SPEED_MPS, 30f).isEmpty());
        fixture.observe(28.0, WALKING_SPEED_MPS, 30f, false, false).assertUncertain();
        List<NavigationTurnEvent> recovered = fixture.state.buildInitialTurnEventIfNeeded(
                fixture.route, fixture.index, position, WALKING_SPEED_MPS, 4f);
        assertEquals(1, recovered.size());
        assertEquals(NavigationTurnEvent.Type.INITIAL, recovered.get(0).type);
        assertTrue(fixture.state.buildInitialTurnEventIfNeeded(fixture.route, fixture.index,
                position, WALKING_SPEED_MPS, 4f).isEmpty());
    }

    private static final class Fixture {
        private final NavigationTurnState state = new NavigationTurnState();
        private final GeoJsonRoute route;
        private final PolylineIndex index;
        private long nowMs;

        private Fixture(int firstCommand) {
            route = new GeoJsonRoute(
                    Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001),
                            new LatLon(0.0, 0.002), new LatLon(0.0, 0.003)),
                    Arrays.asList(new VoiceHint(1, firstCommand, 0, 0.0, 0),
                            new VoiceHint(2, 2, 0, 0.0, -90)),
                    Arrays.asList(0.0, 77.0, 154.0, 231.0), 231.0, 333.0);
            index = new PolylineIndex(route.track);
            state.onRouteApplied(route, index, Collections.emptyList(),
                    new LatLon(0.0, 0.0), WALKING_SPEED_MPS, 4f);
        }

        private Observation observe(double distanceMeters, float speedMps, float accuracyMeters,
                boolean singleInstructionMode, boolean trustworthyProgress) {
            double alongTrackMeters = index.distanceAtPointIndex(1) - distanceMeters;
            int segmentIndex = distanceMeters < 0.0 ? 1 : 0;
            nowMs += 3_000L;
            NavigationTurnState.Progress progress = state.evaluate(route, index, alongTrackMeters,
                    segmentIndex, speedMps, Float.NaN, trustworthyProgress, nowMs, 0L, singleInstructionMode);
            List<NavUpcomingHint> upcoming = NavUpcomingHintCollector.collect(route, index,
                    alongTrackMeters, state.getNextHintIdx(), segmentIndex, speedMps, 2);
            NavGuidanceStatus guidance = NavStateTextFactory.buildGuidanceStatus(route, index,
                    alongTrackMeters, state.getNextHintIdx(), segmentIndex, speedMps, accuracyMeters,
                    false, -1, Collections.emptyList(), TestNavigationTextResources.metric());
            return new Observation(progress.turnEvents, upcoming, guidance);
        }
    }

    private static final class Observation {
        private final List<NavigationTurnEvent> events;
        private final List<NavUpcomingHint> upcoming;
        private final NavGuidanceStatus guidance;

        private Observation(List<NavigationTurnEvent> events, List<NavUpcomingHint> upcoming,
                NavGuidanceStatus guidance) {
            this.events = events;
            this.upcoming = upcoming;
            this.guidance = guidance;
        }

        private void assertUncertain() {
            assertTrue(guidance.nextUncertain);
            assertEquals(1, upcoming.get(0).hint.indexInTrack);
        }

        private void assertCurrentAlert() {
            assertEquals(1, events.size());
            assertEquals(NavigationTurnEvent.Type.IMMINENT, events.get(0).type);
            assertEquals(events.get(0).hint.indexInTrack, upcoming.get(0).hint.indexInTrack);
        }
    }
}
