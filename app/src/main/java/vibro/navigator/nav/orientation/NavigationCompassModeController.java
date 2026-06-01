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
    private Boolean overrideSixtySecondView;
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
        onCompassTapped(automaticState, elapsedRealtimeClock.elapsedRealtimeMs());
    }

    public void onCompassTapped(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        if (automaticState == null) {
            return;
        }
        boolean automaticSixtySecondView = automaticState.displayMode.movingScaleActive;
        boolean displayedSixtySecondView = resolveDisplayedMode(automaticSixtySecondView, nowElapsedMs);
        boolean targetSixtySecondView = !displayedSixtySecondView;
        startRadiusTransition(nowElapsedMs);
        if (targetSixtySecondView == automaticSixtySecondView) {
            clearOverride();
            return;
        }
        overrideSixtySecondView = targetSixtySecondView;
        overrideExpiryElapsedMs = automaticSixtySecondView && !targetSixtySecondView
                ? nowElapsedMs + MOVING_FULL_ROUTE_RESTORE_DELAY_MS
                : NO_EXPIRY;
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState) {
        return resolve(automaticState, elapsedRealtimeClock.elapsedRealtimeMs());
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        if (automaticState == null) {
            clear();
            return null;
        }
        boolean automaticSixtySecondView = automaticState.displayMode.movingScaleActive;
        Boolean displayedSixtySecondView = resolveOverrideMode(automaticSixtySecondView, nowElapsedMs);
        NavCompassState targetState = displayedSixtySecondView == null
                ? automaticState
                : automaticState.withDisplayMode(displayedSixtySecondView);
        return resolveTransitionedState(automaticState, targetState, nowElapsedMs);
    }

    public boolean isTransitionInProgress() {
        return radiusTransitionActive;
    }

    private boolean resolveDisplayedMode(boolean automaticSixtySecondView, long nowElapsedMs) {
        Boolean overrideMode = resolveOverrideMode(automaticSixtySecondView, nowElapsedMs);
        return overrideMode != null ? overrideMode : automaticSixtySecondView;
    }

    @Nullable
    private Boolean resolveOverrideMode(boolean automaticSixtySecondView, long nowElapsedMs) {
        if (overrideSixtySecondView == null) {
            return null;
        }
        if (overrideExpiryElapsedMs != NO_EXPIRY && nowElapsedMs >= overrideExpiryElapsedMs) {
            clearOverride();
            startRadiusTransition(nowElapsedMs);
            return null;
        }
        if (overrideSixtySecondView == automaticSixtySecondView) {
            clearOverride();
            return null;
        }
        return overrideSixtySecondView;
    }

    @NonNull
    private NavCompassState resolveTransitionedState(
            @NonNull NavCompassState automaticState,
            @NonNull NavCompassState targetState,
            long nowElapsedMs
    ) {
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

    private void startRadiusTransition(long nowElapsedMs) {
        radiusTransitionActive = true;
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
        overrideSixtySecondView = null;
        overrideExpiryElapsedMs = NO_EXPIRY;
    }
}
