package vibro.navigator.nav.orientation;


import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationCompassModeController {

    private static final long NO_EXPIRY = -1L;
    private static final long NO_UPDATE_TIME = -1L;
    private static final long MOVING_FULL_ROUTE_RESTORE_DELAY_MS = 5_000L;
    private static final float TARGET_TOLERANCE_RATIO = 0.002f;
    private static final float TARGET_TOLERANCE_METERS = 0.5f;

    @Nullable
    private Boolean overrideMovingScaleView;
    private long overrideExpiryElapsedMs = NO_EXPIRY;
    private boolean radiusTransitionActive;
    @Nullable
    private Float lastResolvedVisibleRadiusMeters;
    private long lastRadiusTransitionUpdateElapsedMs = NO_UPDATE_TIME;
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
        boolean automaticMovingScaleView = automaticState.displayMode.movingScaleActive;
        boolean displayedMovingScaleView = resolveDisplayedMode(
                automaticMovingScaleView,
                nowElapsedMs,
                animateRadiusTransition
        );
        boolean targetMovingScaleView = !displayedMovingScaleView;
        startRadiusTransition(nowElapsedMs, animateRadiusTransition);
        if (targetMovingScaleView == automaticMovingScaleView) {
            clearOverride();
            return;
        }
        overrideMovingScaleView = targetMovingScaleView;
        overrideExpiryElapsedMs = automaticMovingScaleView && !targetMovingScaleView
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
        boolean automaticMovingScaleView = automaticState.displayMode.movingScaleActive;
        Boolean displayedMovingScaleView = resolveOverrideMode(
                automaticMovingScaleView,
                nowElapsedMs,
                animateRadiusTransition
        );
        NavCompassState targetState = displayedMovingScaleView == null
                ? automaticState
                : automaticState.withDisplayMode(displayedMovingScaleView);
        return resolveTransitionedState(
                automaticState,
                targetState,
                nowElapsedMs,
                animateRadiusTransition
        );
    }

    public boolean isTransitionInProgress() {
        return radiusTransitionActive;
    }

    private boolean resolveDisplayedMode(
            boolean automaticMovingScaleView,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        Boolean overrideMode = resolveOverrideMode(
                automaticMovingScaleView,
                nowElapsedMs,
                animateRadiusTransition
        );
        return overrideMode != null ? overrideMode : automaticMovingScaleView;
    }

    @Nullable
    private Boolean resolveOverrideMode(
            boolean automaticMovingScaleView,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        if (overrideMovingScaleView == null) {
            return null;
        }
        if (overrideExpiryElapsedMs != NO_EXPIRY && nowElapsedMs >= overrideExpiryElapsedMs) {
            clearOverride();
            startRadiusTransition(nowElapsedMs, animateRadiusTransition);
            return null;
        }
        if (overrideMovingScaleView == automaticMovingScaleView) {
            clearOverride();
            return null;
        }
        return overrideMovingScaleView;
    }

    @NonNull
    private NavCompassState resolveTransitionedState(
            @NonNull NavCompassState automaticState,
            @NonNull NavCompassState targetState,
            long nowElapsedMs,
            boolean animateRadiusTransition
    ) {
        if (!animateRadiusTransition) {
            radiusTransitionActive = false;
            rememberResolvedRadius(targetState.radiusState.visibleRadiusMeters, nowElapsedMs);
            return targetState;
        }
        if (!radiusTransitionActive) {
            rememberResolvedRadius(targetState.radiusState.visibleRadiusMeters, nowElapsedMs);
            return targetState;
        }

        float previousRadiusMeters = lastResolvedVisibleRadiusMeters != null
                ? lastResolvedVisibleRadiusMeters
                : automaticState.radiusState.visibleRadiusMeters;
        long deltaMs = lastRadiusTransitionUpdateElapsedMs == NO_UPDATE_TIME
                ? 0L
                : Math.max(0L, nowElapsedMs - lastRadiusTransitionUpdateElapsedMs);
        float resolvedRadiusMeters = deltaMs <= 0L
                ? previousRadiusMeters
                : NavCompassStateFactory.smoothVisibleRadiusMeters(
                        targetState.radiusState.visibleRadiusMeters,
                        previousRadiusMeters,
                        deltaMs
                );
        if (isAtTarget(resolvedRadiusMeters, targetState.radiusState.visibleRadiusMeters)) {
            radiusTransitionActive = false;
            rememberResolvedRadius(targetState.radiusState.visibleRadiusMeters, nowElapsedMs);
            return targetState;
        }
        rememberResolvedRadius(resolvedRadiusMeters, nowElapsedMs);
        return targetState.withDisplayMode(targetState.displayMode.movingScaleActive, resolvedRadiusMeters);
    }

    private void startRadiusTransition(long nowElapsedMs, boolean animateRadiusTransition) {
        radiusTransitionActive = animateRadiusTransition;
        lastRadiusTransitionUpdateElapsedMs = nowElapsedMs;
    }

    private void rememberResolvedRadius(float visibleRadiusMeters, long nowElapsedMs) {
        lastResolvedVisibleRadiusMeters = visibleRadiusMeters;
        lastRadiusTransitionUpdateElapsedMs = nowElapsedMs;
    }

    private static boolean isAtTarget(float resolvedRadiusMeters, float targetRadiusMeters) {
        float toleranceMeters = Math.max(
                TARGET_TOLERANCE_METERS,
                Math.abs(targetRadiusMeters) * TARGET_TOLERANCE_RATIO
        );
        return Math.abs(resolvedRadiusMeters - targetRadiusMeters) <= toleranceMeters;
    }

    private void clear() {
        clearOverride();
        radiusTransitionActive = false;
        lastResolvedVisibleRadiusMeters = null;
        lastRadiusTransitionUpdateElapsedMs = NO_UPDATE_TIME;
    }

    private void clearOverride() {
        overrideMovingScaleView = null;
        overrideExpiryElapsedMs = NO_EXPIRY;
    }
}
