package vibro.navigator.nav.orientation;


import vibro.navigator.nav.compass.CompassPerspectiveScale;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationCompassModeController {

    private enum ViewMode {
        FULL_ROUTE,
        MOVING_2D,
        PERSPECTIVE_3D,
        MOVING_2D_AFTER_PERSPECTIVE;

        private static final ViewMode[] CYCLE = values();

        boolean usesMovingScale() {
            return this != FULL_ROUTE;
        }

        ViewMode next() {
            return CYCLE[(ordinal() + 1) % CYCLE.length];
        }
    }

    private static final long NO_EXPIRY = -1L;
    private static final long MOVING_FULL_ROUTE_RESTORE_DELAY_MS = 5_000L;

    @Nullable
    private ViewMode overrideMode;
    private ViewMode displayedMode = ViewMode.FULL_ROUTE;
    @Nullable
    private NavCompassState cachedAutomaticState;
    private boolean cachedMovingScale;
    @Nullable
    private NavCompassState cachedModeState;
    @Nullable
    private NavCompassState cachedPerspectiveBaseState;
    @Nullable
    private NavCompassState cachedPerspectiveState;
    private long overrideExpiryElapsedMs = NO_EXPIRY;
    private final NavigationCompassUiRadiusTransition radiusTransition =
            new NavigationCompassUiRadiusTransition();
    private final NavigationCompassPerspectiveTransition perspectiveTransition =
            new NavigationCompassPerspectiveTransition();
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;

    public NavigationCompassModeController(@NonNull ElapsedRealtimeClock elapsedRealtimeClock) {
        this.elapsedRealtimeClock = elapsedRealtimeClock;
    }

    public void onCompassTapped(@Nullable NavCompassState automaticState) {
        onCompassTapped(automaticState, elapsedRealtimeClock.elapsedRealtimeMs(), true);
    }

    public void onCompassTapped(@Nullable NavCompassState automaticState, boolean animateRadiusTransition) {
        onCompassTapped(automaticState, elapsedRealtimeClock.elapsedRealtimeMs(), animateRadiusTransition);
    }

    public void onCompassTapped(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        onCompassTapped(automaticState, nowElapsedMs, true);
    }

    public void onCompassTapped(
            @Nullable NavCompassState automaticState,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        if (automaticState == null) {
            return;
        }
        ViewMode automaticMode = automaticMode(automaticState);
        ViewMode currentMode = resolveDisplayedMode(
                automaticMode,
                nowElapsedMs,
                animateRadiusTransition
        );
        ViewMode targetMode = currentMode.next();
        perspectiveTransition.start(targetMode == ViewMode.PERSPECTIVE_3D, nowElapsedMs);
        startRadiusTransitionIfScaleChanges(currentMode, targetMode, nowElapsedMs, animateRadiusTransition);
        displayedMode = targetMode;
        if (targetMode == automaticMode) {
            clearOverride();
            return;
        }
        overrideMode = targetMode;
        overrideExpiryElapsedMs = automaticMode.usesMovingScale() && targetMode == ViewMode.FULL_ROUTE
                ? nowElapsedMs + MOVING_FULL_ROUTE_RESTORE_DELAY_MS
                : NO_EXPIRY;
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState) {
        return resolve(automaticState, elapsedRealtimeClock.elapsedRealtimeMs(), true);
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState, boolean animateRadiusTransition) {
        return resolve(automaticState, elapsedRealtimeClock.elapsedRealtimeMs(), animateRadiusTransition);
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        return resolve(automaticState, nowElapsedMs, true);
    }

    @Nullable
    public NavCompassState resolve(
            @Nullable NavCompassState automaticState,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        if (automaticState == null) {
            clear();
            return null;
        }
        ViewMode automaticMode = automaticMode(automaticState);
        displayedMode = resolveDisplayedMode(
                automaticMode,
                nowElapsedMs,
                animateRadiusTransition
        );
        perspectiveTransition.advance(nowElapsedMs);
        NavCompassState targetState = targetState(automaticState, automaticMode);
        NavCompassState baseState = radiusTransition.resolve(
                automaticState,
                targetState,
                nowElapsedMs,
                animateRadiusTransition
        );
        float progress = perspectiveTransition.progress();
        NavCompassState displayedState = baseState;
        if (progress > 0f) {
            displayedState = perspectiveState(baseState);
        }
        return displayedState;
    }

    public boolean isTransitionInProgress() {
        return radiusTransition.isActive() || perspectiveTransition.isActive();
    }

    public boolean isPerspectiveViewEnabled() {
        return displayedMode == ViewMode.PERSPECTIVE_3D;
    }

    public float perspectiveProgress() {
        return perspectiveTransition.progress();
    }

    @NonNull
    private NavCompassState targetState(
            @NonNull NavCompassState automaticState,
            @NonNull ViewMode automaticMode
    ) {
        if (displayedMode == automaticMode) {
            return automaticState;
        }
        boolean movingScale = displayedMode.usesMovingScale();
        if (cachedAutomaticState != automaticState || cachedMovingScale != movingScale) {
            cachedAutomaticState = automaticState;
            cachedMovingScale = movingScale;
            cachedModeState = automaticState.withDisplayMode(movingScale);
        }
        return cachedModeState;
    }

    @NonNull
    private NavCompassState perspectiveState(@NonNull NavCompassState baseState) {
        if (cachedPerspectiveBaseState != baseState) {
            cachedPerspectiveBaseState = baseState;
            cachedPerspectiveState = baseState.withDisplayMode(
                    baseState.displayMode.movingScaleActive,
                    baseState.radiusState.visibleRadiusMeters
                            * CompassPerspectiveScale.maximumViewportMultiplier()
            );
        }
        return cachedPerspectiveState;
    }

    private void startRadiusTransitionIfScaleChanges(
            @NonNull ViewMode currentMode,
            @NonNull ViewMode targetMode,
            long nowElapsedMs,
            boolean animate
    ) {
        if (currentMode.usesMovingScale() == targetMode.usesMovingScale()) {
            return;
        }
        radiusTransition.start(nowElapsedMs, animate);
    }

    @NonNull
    private static ViewMode automaticMode(@NonNull NavCompassState state) {
        return state.displayMode.movingScaleActive ? ViewMode.MOVING_2D : ViewMode.FULL_ROUTE;
    }

    @NonNull
    private ViewMode resolveDisplayedMode(
            @NonNull ViewMode automaticMode,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        ViewMode activeOverride = resolveOverrideMode(
                automaticMode,
                nowElapsedMs,
                animateRadiusTransition
        );
        return activeOverride != null ? activeOverride : automaticMode;
    }

    @Nullable
    private ViewMode resolveOverrideMode(
            @NonNull ViewMode automaticMode,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        if (overrideMode == null) {
            return null;
        }
        if (overrideExpiryElapsedMs != NO_EXPIRY && nowElapsedMs >= overrideExpiryElapsedMs) {
            clearOverride();
            radiusTransition.start(nowElapsedMs, animateRadiusTransition);
            return null;
        }
        if (overrideMode == automaticMode) {
            clearOverride();
            return null;
        }
        return overrideMode;
    }

    private void clear() {
        clearOverride();
        displayedMode = ViewMode.FULL_ROUTE;
        cachedAutomaticState = null;
        cachedModeState = null;
        cachedPerspectiveBaseState = null;
        cachedPerspectiveState = null;
        radiusTransition.reset();
        perspectiveTransition.reset();
    }

    private void clearOverride() {
        overrideMode = null;
        overrideExpiryElapsedMs = NO_EXPIRY;
    }
}
