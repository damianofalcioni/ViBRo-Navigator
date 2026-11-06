package vibro.navigator.nav.guidance;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationGuidanceDiagnosticsTest {
    private static final String ROUTE_ETA = "route_eta";
    private static final String QUALITY_MARGIN = "quality_margin";
    private final GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, .001)),
            Collections.singletonList(new VoiceHint(1, 2, 90, 0, 0)), 0, 111);
    private final PolylineIndex index = new PolylineIndex(route.track);
    private final NavigationGuidanceDiagnostics diagnostics = new NavigationGuidanceDiagnostics();

    @Test
    public void plannerDistinguishesNotDuePreparatoryClosestAndPreviouslySentAlerts() {
        assertEquals(NavigationGuidanceDecision.Reason.NOT_DUE, plan(0, false, false, true, false, 1).decision.reason);
        TurnEventPlanner.Progress preparatory = plan(80, false, false, true, false, 1);
        assertEquals(NavigationGuidanceDecision.Reason.PREPARATORY_ALERT, preparatory.decision.reason);
        assertEquals(1, preparatory.signals.size());
        assertEquals(NavigationGuidanceDecision.Reason.ALREADY_SENT, plan(80, true, false, true, false, 1).decision.reason);
        assertEquals(NavigationGuidanceDecision.Reason.CLOSEST_ALERT, plan(95, true, false, true, false, 1).decision.reason);
        assertEquals(NavigationGuidanceDecision.Reason.ALREADY_SENT, plan(95, true, true, true, false, 1).decision.reason);
    }

    @Test
    public void singleInstructionModeReportsItsOwnAlertThresholds() {
        assertEquals(NavigationGuidanceDecision.Reason.NOT_DUE, plan(85, false, false, true, true, 1).decision.reason);
        assertEquals(NavigationGuidanceDecision.Reason.CLOSEST_ALERT, plan(91, false, false, true, true, 1).decision.reason);
    }

    @Test
    public void unavailableEtaUnreliableDistanceAndUntrustedProgressHaveDistinctReasons() {
        assertEquals(NavigationGuidanceDecision.Reason.ETA_UNAVAILABLE, plan(0, false, false, true, false, 0).decision.reason);
        assertEquals(NavigationGuidanceDecision.Reason.DISTANCE_UNRELIABLE,
                plan(99.5, false, false, true, false, 1).decision.reason);
        TurnEventPlanner.Progress untrusted = plan(80, false, false, false, false, 1);
        assertEquals(NavigationGuidanceDecision.Reason.UNTRUSTED_PROGRESS, untrusted.decision.reason);
        assertTrue(untrusted.signals.isEmpty());
        assertFalse(untrusted.notified20);
    }

    @Test
    public void arrivalBarrierReportsItsActualTargetAndPreservesAlertFlags() {
        TurnEventPlanner.Progress progress = new TurnEventPlanner().advance(route, index,
                Collections.singletonList(new VoiceHint(1, 101, 0, 0, 0)), Collections.singletonList(100d),
                0, false, false, 95, 0, 1, Float.NaN, true, TurnNotificationPlan.from(false));
        assertEquals(NavigationGuidanceDecision.Reason.ARRIVAL_UNCONFIRMED, progress.decision.reason);
        assertEquals(101, progress.decision.command);
        assertEquals(1, progress.decision.hintTrackIndex);
        assertTrue(progress.signals.isEmpty());
        assertFalse(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void recordsIncludeThePlannerDistanceEtaAndSchedulerCause() {
        String record = diagnostics.record(plan(80, false, false, true, false, 1).decision,
                3_000, QUALITY_MARGIN, true, 1, 1_000);
        assertTrue(record.contains("remainingDistanceMeters=20.0"));
        assertTrue(record.contains("timeToHintSeconds=20.0"));
        assertTrue(record.contains("decision=preparatory_alert"));
        assertTrue(record.contains("intervalReason=" + QUALITY_MARGIN));
    }

    @Test
    public void changedCadenceAndReasonAreNotHiddenByTheRoutineThrottle() {
        NavigationGuidanceDecision pending = plan(0, false, false, true, false, 1).decision;
        assertNotNull(diagnostics.record(pending, 60_000, ROUTE_ETA, true, 0, 1_000));
        assertNull(diagnostics.record(plan(10, false, false, true, false, 1).decision,
                60_000, ROUTE_ETA, true, 0, 2_000));
        assertNotNull(diagnostics.record(pending, 3_000, QUALITY_MARGIN, true, 0, 2_001));
        assertNotNull(diagnostics.record(pending, 3_000, "quality_recovery", true, 0, 2_001));
        NavigationGuidanceDecision alreadySent = plan(80, true, false, true, false, 1).decision;
        assertNotNull(diagnostics.record(alreadySent, 3_000, QUALITY_MARGIN, true, 0, 2_002));
        assertNull(diagnostics.record(alreadySent, 3_000, QUALITY_MARGIN, true, 0, 3_000));
        assertNotNull(diagnostics.record(alreadySent, 3_000, QUALITY_MARGIN, true, 0, 32_002));
    }

    @Test
    public void eventsResetAndClockRollbackRemainObservable() {
        NavigationGuidanceDecision pending = plan(0, false, false, true, false, 1).decision;
        assertNotNull(diagnostics.record(pending, 3_000, ROUTE_ETA, true, 1, 1_000));
        assertNotNull(diagnostics.record(pending, 3_000, ROUTE_ETA, true, 1, 1_001));
        assertNotNull(diagnostics.record(pending, 3_000, ROUTE_ETA, true, 0, 999));
        diagnostics.reset();
        assertNotNull(diagnostics.record(pending, 3_000, ROUTE_ETA, true, 0, 1_002));
    }

    @Test
    public void aPassedFinalHintReportsThatThereIsNoNextTarget() {
        TurnEventPlanner.Progress progress = plan(110, true, true, true, false, 1);
        assertEquals(NavigationGuidanceDecision.Reason.NO_HINT, progress.decision.reason);
        assertEquals(1, progress.signals.size());
        assertEquals(-1, progress.decision.hintTrackIndex);
    }

    private TurnEventPlanner.Progress plan(double along, boolean preparatory, boolean closest,
            boolean trusted, boolean single, float speed) {
        return new TurnEventPlanner().advance(route, index, route.voiceHints, Collections.singletonList(100d),
                0, preparatory, closest, along, 0, speed, Float.NaN, trusted, TurnNotificationPlan.from(single));
    }
}
