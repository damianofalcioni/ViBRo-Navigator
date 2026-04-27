package vibro.navigator.nav;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationCompassModeController {

    private static final long NO_EXPIRY = -1L;
    private static final long MOVING_FULL_ROUTE_RESTORE_DELAY_MS = 5_000L;

    @Nullable
    private Boolean overrideSixtySecondView;
    private long overrideExpiryElapsedMs = NO_EXPIRY;

    public void onCompassTapped(@Nullable NavCompassState automaticState) {
        onCompassTapped(automaticState, SystemClock.elapsedRealtime());
    }

    public void onCompassTapped(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        if (automaticState == null) {
            return;
        }
        boolean automaticSixtySecondView = automaticState.movingScaleActive;
        boolean displayedSixtySecondView = resolveDisplayedMode(automaticSixtySecondView, nowElapsedMs);
        boolean targetSixtySecondView = !displayedSixtySecondView;
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
        return resolve(automaticState, SystemClock.elapsedRealtime());
    }

    @Nullable
    public NavCompassState resolve(@Nullable NavCompassState automaticState, long nowElapsedMs) {
        if (automaticState == null) {
            clearOverride();
            return null;
        }
        boolean automaticSixtySecondView = automaticState.movingScaleActive;
        Boolean displayedSixtySecondView = resolveOverrideMode(automaticSixtySecondView, nowElapsedMs);
        if (displayedSixtySecondView == null) {
            return automaticState;
        }
        return automaticState.withDisplayMode(displayedSixtySecondView);
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
            return null;
        }
        if (overrideSixtySecondView == automaticSixtySecondView) {
            clearOverride();
            return null;
        }
        return overrideSixtySecondView;
    }

    private void clearOverride() {
        overrideSixtySecondView = null;
        overrideExpiryElapsedMs = NO_EXPIRY;
    }
}
