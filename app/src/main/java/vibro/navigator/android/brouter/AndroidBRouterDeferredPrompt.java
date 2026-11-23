package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.os.CancellationSignal;

import androidx.core.util.Consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;

/** Owns an instruction dialog from the initial tap through dismissal. */
final class AndroidBRouterDeferredPrompt implements Application.ActivityLifecycleCallbacks {
    interface LocationQuery {
        AndroidBRouterPickerLocation find(CancellationSignal signal, Consumer<AndroidBRouterPickerLocation> progress);
    }

    interface DialogFactory {
        AlertDialog show(AndroidBRouterPickerLocation location);
    }

    private static final Map<Activity, AndroidBRouterDeferredPrompt> PENDING = new HashMap<>();

    private final Activity activity;
    private final DialogFactory show;
    private final int requestCode;
    private boolean paused;
    private boolean showing;
    private AlertDialog dialog;
    private AndroidBRouterPickerLocation location;

    private AndroidBRouterDeferredPrompt(Activity activity, int requestCode, DialogFactory show) {
        this.activity = activity;
        this.requestCode = requestCode;
        this.show = show;
    }

    static void request(Activity activity, int requestCode, boolean replacePending,
                        LocationQuery query, DialogFactory show) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        AndroidBRouterDeferredPrompt previous = PENDING.get(activity);
        if (previous != null && !previous.replace(requestCode, replacePending)) {
            return;
        }
        AndroidBRouterDeferredPrompt prompt = new AndroidBRouterDeferredPrompt(activity, requestCode, show);
        PENDING.put(activity, prompt);
        activity.getApplication().registerActivityLifecycleCallbacks(prompt);
        AndroidTaskScheduler.main().postDelayed(() -> prompt.discover(query), ViewConfiguration.getPressedStateDuration());
    }

    static void cancel(Activity activity, int requestCode) {
        AndroidBRouterDeferredPrompt prompt = PENDING.get(activity);
        if (prompt != null && prompt.requestCode == requestCode) {
            prompt.dispose();
        }
    }

    private boolean replace(int nextRequestCode, boolean replacePending) {
        if (!replacePending || showing || requestCode == nextRequestCode) {
            return false;
        }
        dispose();
        return true;
    }

    private void discover(LocationQuery query) {
        if (equals(PENDING.get(activity))) {
            AtomicReference<AndroidBRouterPickerLocation> fallback = new AtomicReference<>(AndroidBRouterPickerLocation.browse());
            AndroidBRouterFolderDiscovery.findWithFallback(signal -> query.find(signal, fallback::set), fallback::get, result -> {
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
        showing = true;
        dialog = show.show(location);
        location = null;
        if (dialog == null) {
            dispose();
        } else {
            dialog.setOnDismissListener(ignored -> dispose());
        }
    }

    private void dispose() {
        if (equals(PENDING.get(activity))) {
            PENDING.remove(activity);
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
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
