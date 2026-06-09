package vibro.navigator.nav.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.orientation.GeomagneticOrientationMonitor;
import vibro.navigator.nav.orientation.NavigationHeadingMonitor;
import vibro.navigator.nav.orientation.NavigationOrientationController;

final class NoOpNavigationOrientation {
    private NoOpNavigationOrientation() {
    }

    @NonNull
    static NavigationOrientationController create(long nowMs) {
        return new NavigationOrientationController(
                callback -> new NoOpHeadingMonitor(),
                () -> 0,
                () -> nowMs,
                runnable -> runnable.run(),
                new NoOpCompassUiState()
        );
    }

    private static final class NoOpHeadingMonitor implements NavigationHeadingMonitor {
        @Override
        public boolean start() {
            return false;
        }

        @Override
        public void stop() {
        }

        @Nullable
        @Override
        public GeomagneticOrientationMonitor.Sample getLatestSample() {
            return null;
        }
    }

    private static final class NoOpCompassUiState implements NavigationOrientationController.CompassUiState {
        @Override
        public boolean shouldDispatchCompassUi() {
            return false;
        }

        @Override
        public boolean hasStateListeners() {
            return false;
        }

        @Override
        public void requestStateRefresh() {
        }
    }
}
