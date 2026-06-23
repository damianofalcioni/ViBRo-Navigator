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
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }

        assertTrue(controller.getFastChecksUntilMs() < 14_000L);
    }

    @Test
    public void recordEvaluation_keepsWarmupActiveForEarlyStableCallbacks() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 5; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i);
        }

        assertEquals(61_000L, controller.getFastChecksUntilMs());
    }

    @Test
    public void recordEvaluation_resetsStableCountWhenAccuracyIsPoor() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 4; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }
        controller.recordEvaluation(true, 40f, 3_000L);
        controller.recordEvaluation(true, 10f, 13_001L);
        controller.recordEvaluation(true, 10f, 16_001L);
        controller.recordEvaluation(true, 10f, 19_001L);
        controller.recordEvaluation(true, 10f, 22_001L);

        assertEquals(61_000L, controller.getFastChecksUntilMs());
    }

    @Test
    public void onRouteApplied_resetsStableCount() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 4; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }
        controller.onRouteApplied(12_000L);
        controller.recordEvaluation(true, 10f, 12_000L);
        controller.recordEvaluation(true, 10f, 15_000L);
        controller.recordEvaluation(true, 10f, 18_000L);
        controller.recordEvaluation(true, 10f, 21_000L);

        assertEquals(72_000L, controller.getFastChecksUntilMs());
    }

    @Test
    public void onRouteApplied_restartsFastPollingAfterDynamicIntervalExited() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 5; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }
        controller.onRouteApplied(10_000L);

        assertEquals(70_000L, controller.fastChecksUntilMsForEvaluation(10_100L));
    }

    @Test
    public void fastChecksUntilMsForEvaluation_resumesFastPollingAfterLongEvaluationGap() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 5; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }

        long fastChecksUntilMs = controller.fastChecksUntilMsForEvaluation(30_000L);

        assertEquals(90_000L, fastChecksUntilMs);
    }

    @Test
    public void fastChecksUntilMsForEvaluation_keepsDynamicIntervalAfterShortEvaluationGap() {
        NavigationWarmupController controller = new NavigationWarmupController();
        controller.reset(1_000L);

        for (int i = 0; i < 5; i++) {
            controller.recordEvaluation(true, 10f, 2_000L + i * 3_000L);
        }

        long fastChecksUntilMs = controller.fastChecksUntilMsForEvaluation(20_000L);

        assertTrue(fastChecksUntilMs < 20_000L);
    }
}
