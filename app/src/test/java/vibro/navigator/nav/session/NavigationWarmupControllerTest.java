package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationWarmupControllerTest {

    @Test
    public void reset_keepsFastPollingActiveForWarmupWindow() {
        NavigationWarmupController controller = new NavigationWarmupController();

        controller.reset(1_000L);

        assertEquals(61_000L, controller.getFastChecksUntilMs());
    }

    @Test
    public void recordEvaluation_exitsWarmupEarlyAfterConsecutiveStableSamples() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 5; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i);
        }

        assertTrue(controller.getFastChecksUntilMs() < 2_004L);
    }

    @Test
    public void recordEvaluation_resetsStableCountWhenAccuracyIsPoor() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 4; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i);
        }
        controller.recordEvaluation(true, 40f, 3_000L);
        controller.recordEvaluation(true, 10f, 3_001L);
        controller.recordEvaluation(true, 10f, 3_002L);
        controller.recordEvaluation(true, 10f, 3_003L);
        controller.recordEvaluation(true, 10f, 3_004L);

        assertEquals(61_000L, controller.getFastChecksUntilMs());
    }

    @Test
    public void onRouteApplied_resetsStableCount() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 4; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i);
        }
        controller.onRouteApplied();
        controller.recordEvaluation(true, 10f, 3_000L);
        controller.recordEvaluation(true, 10f, 3_001L);
        controller.recordEvaluation(true, 10f, 3_002L);
        controller.recordEvaluation(true, 10f, 3_003L);

        assertEquals(61_000L, controller.getFastChecksUntilMs());
    }
}
