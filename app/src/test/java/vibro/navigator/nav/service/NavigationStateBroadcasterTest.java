package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import vibro.navigator.nav.model.NavState;

public class NavigationStateBroadcasterTest {

    @Test
    public void dispatchStoppedNotifiesRegisteredListeners() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        StopRecordingListener listener = new StopRecordingListener();
        broadcaster.register(listener);

        broadcaster.dispatchStopped();

        assertEquals(1, listener.stopCount);
    }

    @Test
    public void dispatchStoppedContinuesAfterListenerFailure() {
        NavigationStateBroadcaster broadcaster = new NavigationStateBroadcaster();
        StopRecordingListener listener = new StopRecordingListener();
        broadcaster.register(new ThrowingStopListener());
        broadcaster.register(listener);

        broadcaster.dispatchStopped();

        assertEquals(1, listener.stopCount);
    }

    private static class StopRecordingListener implements NavigationService.Listener {
        int stopCount;

        @Override
        public void onState(@NonNull NavState state) {
        }

        @Override
        public void onNavigationStopped() {
            stopCount++;
        }
    }

    private static final class ThrowingStopListener extends StopRecordingListener {
        @Override
        public void onNavigationStopped() {
            throw new IllegalStateException("listener failure");
        }
    }
}
