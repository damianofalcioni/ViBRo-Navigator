package vibro.navigator;


import vibro.navigator.nav.policy.NavigationLifecyclePolicy;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;

import vibro.navigator.util.AppLogger;

final class NavigationActivityBackHandler {

    private static final String TAG = "NavigationActivity";

    private final Activity activity;
    private final NavigationLifecyclePolicy lifecyclePolicy;
    private final Runnable legacyBackFallback;
    private OnBackInvokedCallback backInvokedCallback;

    NavigationActivityBackHandler(
            @NonNull Activity activity,
            @NonNull NavigationLifecyclePolicy lifecyclePolicy,
            @NonNull Runnable legacyBackFallback
    ) {
        this.activity = activity;
        this.lifecyclePolicy = lifecyclePolicy;
        this.legacyBackFallback = legacyBackFallback;
    }

    @SuppressLint("GestureBackNavigation")
    void onLegacyBackPressed() {
        handleBackAction(true);
    }

    void registerPredictiveBackCallbackIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backInvokedCallback != null) {
            return;
        }
        backInvokedCallback = () -> handleBackAction(false);
        activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
        );
    }

    void unregisterPredictiveBackCallbackIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backInvokedCallback == null) {
            return;
        }
        activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
        backInvokedCallback = null;
    }

    private void handleBackAction(boolean legacyBackPress) {
        NavigationLifecyclePolicy.BackPressAction action = lifecyclePolicy.onNavigationBackPressed();
        if (action == NavigationLifecyclePolicy.BackPressAction.MOVE_TASK_TO_BACKGROUND) {
            AppLogger.i(TAG, "Back pressed during navigation, moving task to background");
            if (!activity.moveTaskToBack(true)) {
                activity.finish();
            }
            return;
        }
        if (legacyBackPress) {
            legacyBackFallback.run();
        } else {
            activity.finish();
        }
    }
}
