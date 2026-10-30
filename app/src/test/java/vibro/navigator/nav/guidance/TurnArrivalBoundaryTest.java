package vibro.navigator.nav.guidance;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class TurnArrivalBoundaryTest {
    private final List<LatLon> track = Arrays.asList(new LatLon(0, 0), new LatLon(0, .001),
            new LatLon(0, .002), new LatLon(0, .003), new LatLon(0, .004));
    private final GeoJsonRoute route = new GeoJsonRoute(track, Arrays.asList(
            new VoiceHint(1, 5, 0, 0, 0), new VoiceHint(3, 2, 0, 0, 90)), 100, 444);
    private final PolylineIndex index = new PolylineIndex(track);

    @Test
    public void sparseUpdateCannotEmitOrConsumeTurnsBeyondUnconfirmedStop() {
        for (boolean single : new boolean[] {false, true}) {
            NavigationTurnState state = new NavigationTurnState();
            state.onRouteApplied(route, index, Collections.singletonList(track.get(2)), track.get(0), 10, 5);
            NavigationTurnState.Progress progress = state.evaluate(route, index, 350, 3, 10, 10_000, 0, single);
            assertEquals(1, progress.turnEvents.size());
            assertEquals(1, progress.turnEvents.get(0).hint.indexInTrack);
            assertEquals(NavigationTurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
            assertTrue(state.evaluate(route, index, 350, 3, 10, 13_000, 0, single).turnEvents.isEmpty());
            state.onIntermediateDestinationReached(2);
            assertEquals(3, state.evaluate(route, index, 350, 3, 10, 16_000, 0, single)
                    .turnEvents.get(0).hint.indexInTrack);
        }
    }

    @Test
    public void plannerStopsBeforeArrivalAndLeavesLaterAlertFlagsUnset() {
        List<VoiceHint> hints = Arrays.asList(route.voiceHints.get(0), new VoiceHint(2, 101, 0, 0, 0),
                route.voiceHints.get(1));
        TurnEventPlanner.Progress progress = new TurnEventPlanner().advance(route, index, hints, 0,
                false, false, 250, 2, 10, false);
        assertEquals(1, progress.nextHintIdx);
        assertEquals(1, progress.signals.size());
        assertFalse(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void applyingARoutePastARequiredStopCannotStartWithALaterTurn() {
        NavigationTurnState state = new NavigationTurnState();
        assertTrue(state.onRouteApplied(route, index, Collections.singletonList(track.get(2)),
                new LatLon(0, .00225), 10, 5).isEmpty());
        assertTrue(state.evaluate(route, index, 250, 2, 10, 10_000, 0).turnEvents.isEmpty());
    }

    @Test
    public void confirmingOneColocatedStopPreservesTheSecondArrivalBarrier() {
        NavigationTurnState state = new NavigationTurnState();
        state.onRouteApplied(route, index, Arrays.asList(track.get(2), track.get(2)), track.get(0), 10, 5);
        state.evaluate(route, index, 120, 1, 10, 1_000, 0);
        state.onIntermediateDestinationReached(2);
        assertTrue(state.evaluate(route, index, 250, 2, 10, 4_000, 0).turnEvents.isEmpty());
        state.onIntermediateDestinationReached(2);
        assertEquals(3, state.evaluate(route, index, 250, 2, 10, 7_000, 0)
                .turnEvents.get(0).hint.indexInTrack);
    }
}
