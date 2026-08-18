package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.session.NavigationSession;

public class NavigationServiceUiVisibilityTest {

    @Test
    public void screenOffClearsCompassStreetViewportAndRejectsNewViewport() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable stateRefresh = new CountingRunnable();
        CountingRunnable viewportClearer = new CountingRunnable();
        DisplayActivityRecorder displayActivity = new DisplayActivityRecorder();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                stateRefresh,
                viewportClearer,
                displayActivity
        );
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();
        visibility.setNavigationUiVisible(true);

        assertTrue(visibility.canUseCompassStreetViewport());
        assertTrue(visibility.canDispatchStateToUi());
        assertTrue(displayActivity.active);

        int priorViewportClears = viewportClearer.calls;
        visibility.onScreenInteractiveChanged(false);

        assertEquals(priorViewportClears + 1, viewportClearer.calls);
        assertFalse(visibility.canUseCompassStreetViewport());
        assertFalse(visibility.canDispatchStateToUi());
        assertFalse(displayActivity.active);
    }

    @Test
    public void hiddenNavigationUiClearsCompassStreetViewport() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable stateRefresh = new CountingRunnable();
        CountingRunnable viewportClearer = new CountingRunnable();
        DisplayActivityRecorder displayActivity = new DisplayActivityRecorder();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                stateRefresh,
                viewportClearer,
                displayActivity
        );
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();
        visibility.setNavigationUiVisible(true);

        int priorViewportClears = viewportClearer.calls;
        visibility.setNavigationUiVisible(false);

        assertEquals(priorViewportClears + 1, viewportClearer.calls);
        assertFalse(visibility.canUseCompassStreetViewport());
        assertFalse(visibility.canDispatchStateToUi());
        assertFalse(displayActivity.active);
    }

    @Test
    public void visibleInteractiveUiNeedsAStateListenerForStreetViewport() {
        CountingRunnable viewportClearer = new CountingRunnable();
        NavigationServiceUiVisibility visibility = visibility(
                new NavigationStateBroadcaster(),
                new CountingRunnable(),
                viewportClearer
        );

        visibility.setNavigationUiVisible(true);

        assertFalse(visibility.canUseCompassStreetViewport());
        assertEquals(1, viewportClearer.calls);
    }

    @Test
    public void listenerRegistrationActivatesVisibleInteractiveDisplay() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        DisplayActivityRecorder displayActivity = new DisplayActivityRecorder();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                new CountingRunnable(),
                new CountingRunnable(),
                displayActivity
        );

        visibility.setNavigationUiVisible(true);
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();

        assertTrue(displayActivity.active);
        assertEquals(1, displayActivity.calls);
    }

    @Test
    public void carNavigationUiDispatchesWhilePhoneScreenIsOff() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable stateRefresh = new CountingRunnable();
        CountingRunnable viewportClearer = new CountingRunnable();
        DisplayActivityRecorder displayActivity = new DisplayActivityRecorder();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                stateRefresh,
                viewportClearer,
                displayActivity
        );
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();
        visibility.onScreenInteractiveChanged(false);

        visibility.setCarNavigationUiVisible(true);

        assertTrue(visibility.hasActiveNavigationDisplay());
        assertTrue(visibility.canUseCompassStreetViewport());
        assertTrue(visibility.canDispatchStateToUi());
        assertTrue(displayActivity.active);
        assertEquals(1, stateRefresh.calls);
    }

    @Test
    public void screenOffWithoutCarNavigationUiKeepsBatteryDisplayGateClosed() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable stateRefresh = new CountingRunnable();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                stateRefresh,
                new CountingRunnable()
        );
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();
        visibility.setNavigationUiVisible(true);
        stateRefresh.calls = 0;

        visibility.onScreenInteractiveChanged(false);

        assertFalse(visibility.hasActiveNavigationDisplay());
        assertFalse(visibility.canDispatchStateToUi());
        assertEquals(0, stateRefresh.calls);
    }

    @Test
    public void disconnectingCarNavigationUiRestoresScreenOffDisplayGate() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable viewportClearer = new CountingRunnable();
        DisplayActivityRecorder displayActivity = new DisplayActivityRecorder();
        NavigationServiceUiVisibility visibility = visibility(
                broadcaster,
                new CountingRunnable(),
                viewportClearer,
                displayActivity
        );
        broadcaster.register(state -> {
        });
        visibility.onStateListenersChanged();
        visibility.onScreenInteractiveChanged(false);
        visibility.setCarNavigationUiVisible(true);

        int priorViewportClears = viewportClearer.calls;
        visibility.setCarNavigationUiVisible(false);

        assertEquals(priorViewportClears + 1, viewportClearer.calls);
        assertFalse(visibility.hasActiveNavigationDisplay());
        assertFalse(visibility.canDispatchStateToUi());
        assertFalse(displayActivity.active);
    }

    @Test
    public void compassRefreshUsesLightweightStateCallback() {
        CountingRunnable structuralRefresh = new CountingRunnable();
        CountingRunnable compassRefresh = new CountingRunnable();
        NavigationServiceUiVisibility visibility = new NavigationServiceUiVisibility(
                new NavigationSession(),
                new NavigationStateBroadcaster(),
                structuralRefresh,
                compassRefresh,
                new CountingRunnable(),
                active -> {
                }
        );

        visibility.requestStateRefresh();

        assertEquals(0, structuralRefresh.calls);
        assertEquals(1, compassRefresh.calls);
    }

    private static NavigationServiceUiVisibility visibility(
            NavigationStateBroadcaster broadcaster,
            CountingRunnable stateRefresh,
            CountingRunnable viewportClearer
    ) {
        return visibility(broadcaster, stateRefresh, viewportClearer, new DisplayActivityRecorder());
    }

    private static NavigationServiceUiVisibility visibility(
            NavigationStateBroadcaster broadcaster,
            CountingRunnable stateRefresh,
            CountingRunnable viewportClearer,
            DisplayActivityRecorder displayActivity
    ) {
        return new NavigationServiceUiVisibility(
                new NavigationSession(),
                broadcaster,
                stateRefresh,
                viewportClearer,
                displayActivity
        );
    }

    private static final class CountingRunnable implements Runnable {
        private int calls;

        @Override
        public void run() {
            calls++;
        }
    }

    private static final class DisplayActivityRecorder implements NavigationServiceUiVisibility.DisplayActivityListener {
        private boolean active;
        private int calls;

        @Override
        public void onDisplayActivityChanged(boolean active) {
            this.active = active;
            calls++;
        }
    }
}
