package vibro.navigator.nav.ui;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class NavigationBlockedRoadButton {
    private static final float BUTTON_ENABLED_ALPHA = 1f;
    private static final float BLOCKED_ROAD_DISABLED_ALPHA = 0.45f;

    @NonNull
    private final Activity activity;
    @NonNull
    private final ImageButton button;
    @Nullable
    private Boolean lastEnabled;
    @Nullable
    private Boolean lastSkipsBeelineTarget;

    NavigationBlockedRoadButton(@NonNull Activity activity) {
        this.activity = activity;
        button = activity.findViewById(R.id.blockedRoadButton);
    }

    void setOnClickListener(@NonNull View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    void render(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        updateContentDescription(isBeelineTargetActive(state));
        updateEnabled(navBinder != null
                && state.routeStatus.blockedRoadActionAvailable
                && !state.pauseStatus.paused);
    }

    private void updateContentDescription(boolean skipsBeelineTarget) {
        if (lastSkipsBeelineTarget == null || lastSkipsBeelineTarget != skipsBeelineTarget) {
            lastSkipsBeelineTarget = skipsBeelineTarget;
            button.setContentDescription(activity.getString(
                    skipsBeelineTarget ? R.string.action_skip_beeline_target : R.string.action_blocked_road
            ));
        }
    }

    private void updateEnabled(boolean enabled) {
        if (lastEnabled != null && lastEnabled == enabled) {
            return;
        }
        lastEnabled = enabled;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? BUTTON_ENABLED_ALPHA : BLOCKED_ROAD_DISABLED_ALPHA);
        if (enabled) {
            button.clearColorFilter();
            return;
        }
        button.setColorFilter(
                AndroidAppTheme.color(activity, R.attr.vibroTextSecondaryColor),
                PorterDuff.Mode.SRC_IN
        );
    }

    private static boolean isBeelineTargetActive(@NonNull NavState state) {
        if (!state.routeStatus.blockedRoadActionAvailable
                || state.routeStatus.compassState == null) {
            return false;
        }
        return state.routeStatus.compassState.displayMode.straightLineMode
                || state.routeStatus.compassState.routeStartApproachProjection != null;
    }
}
