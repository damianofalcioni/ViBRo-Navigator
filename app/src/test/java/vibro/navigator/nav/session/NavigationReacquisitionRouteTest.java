package vibro.navigator.nav.session;

import java.util.Collections;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavigationRequest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class NavigationReacquisitionRouteTest extends NavigationSessionRouteStateTestSupport {
    @Test
    public void correctionBurstDoesNotRerouteAdvanceTurnsOrCommitDeparture() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(TREKKING_PROFILE, DESTINATION,
                new LatLon(0, 0.003), Collections.emptyList());
        state.applyRouteResult(TestNavigationTextResources.metric(), snapshot(request), routeWithoutHints(),
                location(0, 0, 1_000), 5, 500);
        state.evaluateLocation(location(0, 0.0002, 1_000), 1.3f, false, 15, null, 1_000, 0);
        double[] distances = {105.27, 72.57, 50.03, 40.92, 37.66};
        long[] times = {63_820, 66_022, 67_023, 68_028, 69_842};
        float[] accuracies = {21.939f, 41.575f, 31.009f, 26.853f, 20.815f};
        for (int i = 0; i < distances.length; i++) {
            var fix = location(distances[i] / 111_320, 0.0008, times[i], accuracies[i]);
            NavigationRouteEvaluation evaluation = state.evaluateLocation(fix, 1.3f, false,
                    accuracies[i], null, times[i], 0);
            assertFalse(evaluation.shouldRecalculateRoute());
            assertFalse(evaluation.shouldSpeculativelyRecalculateRoute());
            assertEquals(3_000, evaluation.getSuggestedUpdateIntervalMs());
            assertEquals(0, evaluation.turnEvents.size());
        }
    }
}
