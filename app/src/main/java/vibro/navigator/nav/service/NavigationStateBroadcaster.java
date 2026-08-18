package vibro.navigator.nav.service;


import vibro.navigator.nav.model.NavState;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.session.NavigationSessionResourceAdapter;

import java.util.ArrayList;
import java.util.List;

public final class NavigationStateBroadcaster {

    private final List<NavigationService.Listener> listeners = new ArrayList<>();
    private final NavigationServiceStateCache stateCache = new NavigationServiceStateCache();

    public void register(@NonNull NavigationService.Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(@NonNull NavigationService.Listener listener) {
        listeners.remove(listener);
    }

    public void clear() {
        listeners.clear();
        stateCache.clear();
    }

    public int size() {
        return listeners.size();
    }

    public void dispatch(@NonNull NavState state) {
        for (NavigationService.Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onState(state);
            } catch (Exception ignored) {
                // Listener failures must not break navigation updates for other listeners.
            }
        }
    }

    public void dispatchStructural(
            @NonNull NavState state,
            @Nullable CompassOrientationCue orientationCue
    ) {
        stateCache.storeStructuralState(state, orientationCue);
        dispatch(state);
    }

    boolean dispatchHeadingIfPossible(
            @NonNull NavigationSession navigationSession,
            @Nullable NavigationServiceRuntime runtime,
            boolean displayActive
    ) {
        if (runtime == null || !displayActive) {
            return true;
        }
        if (!stateCache.canRefreshHeadingOnly(runtime.activeOrientationCue())) {
            return false;
        }
        NavState cachedState = stateCache.currentState();
        if (cachedState == null) {
            return false;
        }
        NavState state = NavigationSessionResourceAdapter.withDisplayHeading(
                navigationSession,
                cachedState,
                runtime.displayHeadingDegrees(),
                runtime.displayHeadingAccuracyDegrees()
        );
        stateCache.storeHeadingState(state);
        dispatch(state);
        return true;
    }

    void dispatchHeadingOrRefreshStructural(
            @NonNull NavigationSession navigationSession,
            @Nullable NavigationServiceRuntime runtime,
            boolean displayActive,
            @NonNull Runnable structuralRefresh
    ) {
        if (!dispatchHeadingIfPossible(navigationSession, runtime, displayActive)) {
            structuralRefresh.run();
        }
    }

    public void dispatchStopped() {
        for (NavigationService.Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onNavigationStopped();
            } catch (Exception ignored) {
                // Listener failures must not break navigation stop cleanup for other listeners.
            }
        }
    }
}
