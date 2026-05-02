package vibro.navigator.nav.policy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavigationLifecyclePolicyTest {

    private final NavigationLifecyclePolicy policy = new NavigationLifecyclePolicy();

    @Test
    public void backPressMovesTaskToBackground() {
        assertEquals(
                NavigationLifecyclePolicy.BackPressAction.MOVE_TASK_TO_BACKGROUND,
                policy.onNavigationBackPressed()
        );
    }

    @Test
    public void connectedUiDoesNothingWhenForegroundNotificationAlreadyVisible() {
        assertEquals(
                NavigationLifecyclePolicy.ForegroundAction.PROMOTE_TO_FOREGROUND,
                policy.onNavigationUiConnected(true)
        );
    }

    @Test
    public void connectedUiRestoresForegroundWhenNotificationMissing() {
        assertEquals(
                NavigationLifecyclePolicy.ForegroundAction.PROMOTE_TO_FOREGROUND,
                policy.onNavigationUiConnected(false)
        );
    }

    @Test
    public void foregroundCheckKeepsRunningWhenNotificationVisible() {
        assertEquals(
                NavigationLifecyclePolicy.ForegroundAction.NONE,
                policy.onForegroundNotificationCheck(true)
        );
    }

    @Test
    public void foregroundCheckStopsNavigationWhenNotificationMissing() {
        assertEquals(
                NavigationLifecyclePolicy.ForegroundAction.STOP_NAVIGATION,
                policy.onForegroundNotificationCheck(false)
        );
    }

    @Test
    public void taskRemovedStopsNavigation() {
        assertEquals(
                NavigationLifecyclePolicy.TaskRemovedAction.STOP_NAVIGATION,
                policy.onTaskRemoved()
        );
    }
}
