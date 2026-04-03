package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;

import android.os.Handler;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class NavigationForegroundCoordinatorTest {

    private TestHost host;
    private NavigationForegroundCoordinator coordinator;

    @Before
    public void setUp() {
        host = new TestHost();
        coordinator = new NavigationForegroundCoordinator(
                new Handler(Looper.getMainLooper()),
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
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS);

        assertEquals(1, host.stopNavigationCalls);
        assertEquals(1, host.stopSelfCalls);
    }

    @Test
    public void stopMonitoringCancelsPendingCheck() {
        host.notificationVisible = false;

        coordinator.startMonitoring();
        coordinator.stopMonitoring();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS);

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
}
