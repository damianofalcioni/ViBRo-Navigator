package vibro.navigator.nav.foreground;


import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

public class NavigationForegroundCoordinatorTest {

    private TestHost host;
    private TestScheduler scheduler;
    private NavigationForegroundCoordinator coordinator;

    @Before
    public void setUp() {
        host = new TestHost();
        scheduler = new TestScheduler();
        coordinator = new NavigationForegroundCoordinator(
                scheduler,
                new NavigationLifecyclePolicy(),
                5_000L,
                host
        );
    }

    @Test
    public void uiConnectionPromotesForegroundWhenNotificationMissing() {
        host.notificationVisible = false;

        coordinator.onNavigationUiConnected();

        assertEquals(1, host.promoteCalls);
    }

    @Test
    public void monitoringStopsNavigationWhenNotificationDisappears() {
        host.notificationVisible = false;

        coordinator.startMonitoring();
        scheduler.runDelayedTask();

        assertEquals(1, host.stopNavigationCalls);
        assertEquals(1, host.stopSelfCalls);
    }

    @Test
    public void stopMonitoringCancelsPendingCheck() {
        host.notificationVisible = false;

        coordinator.startMonitoring();
        coordinator.stopMonitoring();
        scheduler.runDelayedTask();

        assertEquals(0, host.stopNavigationCalls);
        assertEquals(0, host.stopSelfCalls);
    }

    private static final class TestHost implements NavigationForegroundCoordinator.Host {
        boolean notificationVisible = true;
        int promoteCalls;
        int stopNavigationCalls;
        int stopSelfCalls;

        @Override
        public boolean isOngoingNotificationVisible() {
            return notificationVisible;
        }

        @Override
        public void promoteToForeground() {
            promoteCalls++;
        }

        @Override
        public void stopNavigation() {
            stopNavigationCalls++;
        }

        @Override
        public void stopSelf() {
            stopSelfCalls++;
        }
    }

    private static final class TestScheduler implements TaskScheduler {
        private Runnable delayedTask;

        @Override
        public void post(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            delayedTask = runnable;
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            if (Objects.equals(delayedTask, runnable)) {
                delayedTask = null;
            }
        }

        private void runDelayedTask() {
            Runnable task = delayedTask;
            delayedTask = null;
            if (task != null) {
                task.run();
            }
        }
    }
}
