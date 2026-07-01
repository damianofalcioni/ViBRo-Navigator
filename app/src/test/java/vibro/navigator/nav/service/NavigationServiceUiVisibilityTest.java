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
        NavigationServiceUiVisibility visibility = visibility(broadcaster, stateRefresh, viewportClearer);
        broadcaster.register(state -> {
        });
        visibility.setNavigationUiVisible(true);

        assertTrue(visibility.canUseCompassStreetViewport());
        assertTrue(visibility.canDispatchStateToUi());

        visibility.onScreenInteractiveChanged(false);

        assertEquals(1, viewportClearer.calls);
        assertFalse(visibility.canUseCompassStreetViewport());
        assertFalse(visibility.canDispatchStateToUi());
    }

    @Test
    public void hiddenNavigationUiClearsCompassStreetViewport() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        CountingRunnable stateRefresh = new CountingRunnable();
        CountingRunnable viewportClearer = new CountingRunnable();
        NavigationServiceUiVisibility visibility = visibility(broadcaster, stateRefresh, viewportClearer);
        broadcaster.register(state -> {
        });
        visibility.setNavigationUiVisible(true);

        visibility.setNavigationUiVisible(false);

        assertEquals(1, viewportClearer.calls);
        assertFalse(visibility.canUseCompassStreetViewport());
        assertFalse(visibility.canDispatchStateToUi());
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

    private static NavigationServiceUiVisibility visibility(
            NavigationStateBroadcaster broadcaster,
            CountingRunnable stateRefresh,
            CountingRunnable viewportClearer
    ) {
        return new NavigationServiceUiVisibility(
                new NavigationSession(),
                broadcaster,
                stateRefresh,
                viewportClearer
        );
    }

    private static final class CountingRunnable implements Runnable {
        private int calls;

        @Override
        public void run() {
            calls++;
        }
    }
}
