package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewConfiguration;

import java.util.HashMap;
import java.util.Map;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;

/** Owns an instruction dialog from the initial tap through dismissal. */
final class AndroidBRouterDeferredPrompt implements Application.ActivityLifecycleCallbacks {
    interface DialogFactory {
        AlertDialog show(AndroidBRouterPickerLocation location);
    }

    private static final Map<Activity, AndroidBRouterDeferredPrompt> PENDING = new HashMap<>();

    private final Activity activity;
    private final DialogFactory show;
    private boolean paused;
    private AndroidBRouterPickerLocation location;

    private AndroidBRouterDeferredPrompt(Activity activity, DialogFactory show) {
        this.activity = activity;
        this.show = show;
    }

    static void request(Activity activity, AndroidBRouterFolderDiscovery.Query<AndroidBRouterPickerLocation> query,
                        DialogFactory show) {
        if (PENDING.containsKey(activity) || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        AndroidBRouterDeferredPrompt prompt = new AndroidBRouterDeferredPrompt(activity, show);
        PENDING.put(activity, prompt);
        activity.getApplication().registerActivityLifecycleCallbacks(prompt);
        AndroidTaskScheduler.main().postDelayed(() -> prompt.discover(query), ViewConfiguration.getPressedStateDuration());
    }

    private void discover(AndroidBRouterFolderDiscovery.Query<AndroidBRouterPickerLocation> query) {
        if (equals(PENDING.get(activity))) {
            AndroidBRouterFolderDiscovery.find(query, AndroidBRouterPickerLocation.browse(), result -> {
                location = result;
                showIfReady();
            });
        }
    }

    private void showIfReady() {
        if (paused || location == null || !equals(PENDING.get(activity))) {
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            dispose();
            return;
        }
        AlertDialog dialog = show.show(location);
        location = null;
        dialog.setOnDismissListener(ignored -> dispose());
    }

    private void dispose() {
        if (equals(PENDING.get(activity))) {
            PENDING.remove(activity);
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    @Override public void onActivityCreated(Activity host, Bundle state) { }
    @Override public void onActivityStarted(Activity host) { }
    @Override public void onActivitySaveInstanceState(Activity host, Bundle state) { }
    @Override public void onActivityStopped(Activity host) { }

    @Override
    public void onActivityResumed(Activity host) {
        if (host == activity) {
            paused = false;
            showIfReady();
        }
    }

    @Override
    public void onActivityPaused(Activity host) {
        if (host == activity) {
            paused = true;
        }
    }

    @Override
    public void onActivityDestroyed(Activity host) {
        if (host == activity) {
            dispose();
        }
    }
}
