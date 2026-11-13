package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;

/** Sequences first-run folder grants and owns manual and automatic main-screen requests. */
public final class AndroidBRouterStorageSetup {
    static final int REQUEST_FOLDER = 1102;
    private static final String TAG = "BRouterStorageSetup";
    private final Activity activity;
    private final AndroidBRouterFolderRequests requests;
    private final AndroidBRouterForegroundChecks foreground;
    private final AndroidBRouterPickerRequest firstRunPicker;
    private final Completion firstRunCompletion;
    private AlertDialog folderDialog;
    private Runnable firstRunOffer;
    private boolean resumed;

    public AndroidBRouterStorageSetup(Activity activity, @Nullable Bundle state) {
        this.activity = activity;
        requests = new AndroidBRouterFolderRequests(activity);
        foreground = new AndroidBRouterForegroundChecks(activity, this, requests, state);
        firstRunPicker = new AndroidBRouterPickerRequest(activity, "brouter_first_run_request");
        firstRunCompletion = firstRunPicker.restore(state);
    }

    public boolean completedFirstRunStep() {
        return firstRunCompletion == Completion.ADVANCE;
    }

    public boolean requestForFirstRun(Runnable onCancel) {
        if (firstRunPicker.matches(REQUEST_FOLDER)) {
            return true;
        }
        if (!requests.needsFolderAccess() || !requests.needsGrant()) {
            return false;
        }
        AndroidTaskScheduler.main().postDelayed(() -> discoverFirstRun(onCancel),
                ViewConfiguration.getPressedStateDuration());
        return true;
    }

    private void discoverFirstRun(Runnable onCancel) {
        if (canShowDialog()) {
            requests.findLocation(Collections.emptySet(), AndroidBRouterPickerLocation.browse(),
                    location -> {
                        firstRunOffer = () -> showFolder(location, () -> firstRunPicker.begin(REQUEST_FOLDER), onCancel);
                        showFirstRunOffer();
                    });
        }
    }

    public void setResumed(boolean resumed) {
        this.resumed = resumed;
        if (resumed) {
            showFirstRunOffer();
        }
    }

    private void showFirstRunOffer() {
        if (resumed && firstRunOffer != null && canShowDialog()) {
            Runnable offer = firstRunOffer;
            firstRunOffer = null;
            offer.run();
        }
    }

    public void bindManualAction(View action) {
        foreground.bindAction(action);
    }

    public void setForegroundChecksEnabled(boolean enabled) {
        foreground.setEnabled(enabled);
    }

    public ArrayList<String> skippedSegmentsFolders() {
        return foreground.offers().copy();
    }

    void showFolder(AndroidBRouterPickerLocation location, Runnable beforeLaunch, Runnable onCancel) {
        Runnable launch = rememberOffer(location, beforeLaunch);
        Runnable cancel = rememberOffer(location, onCancel);
        folderDialog = AndroidBRouterFolderPrompt.show(activity, location.initialUri, REQUEST_FOLDER, TAG,
                launch, cancel, cancel);
    }

    private Runnable rememberOffer(AndroidBRouterPickerLocation location, Runnable callback) {
        return () -> {
            foreground.offers().record(location.confirmedUri());
            callback.run();
        };
    }

    public boolean handleActivityResult(
            int requestCode, int resultCode, @Nullable Intent data, Runnable onAdvance, Runnable onRetry
    ) {
        if (firstRunPicker.matches(requestCode)) {
            requests.handleResult(resultCode, data, firstRunPicker, onAdvance, onRetry);
            return true;
        }
        return foreground.handleResult(requestCode, resultCode, data);
    }

    boolean canShowDialog() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    public void saveState(Bundle state) {
        firstRunPicker.save(state);
        foreground.saveState(state);
    }

    public void dispose() {
        foreground.setEnabled(false);
        if (isDialogShowing()) {
            folderDialog.dismiss();
        }
    }

    boolean isDialogShowing() {
        return folderDialog != null && folderDialog.isShowing();
    }

    void refreshAfterDismiss(Runnable refresh) {
        folderDialog.setOnDismissListener(dialog -> refresh.run());
    }
}
