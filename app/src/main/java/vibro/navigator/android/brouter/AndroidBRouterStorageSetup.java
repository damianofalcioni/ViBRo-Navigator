package vibro.navigator.android.brouter;

import androidx.core.util.Supplier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.brouter.BRouterFolderGrant;

/** Sequences first-run folder grants and owns manual and automatic main-screen requests. */
public final class AndroidBRouterStorageSetup {
    static final int REQUEST_FOLDER = 1102;
    private static final String STATE_FIRST_RUN = "brouter_first_run_request";
    private static final String TAG = "BRouterStorageSetup";
    private final Activity activity;
    private final AndroidBRouterFolderRequests requests;
    private final AndroidBRouterForegroundChecks foreground;
    private final AndroidBRouterPickerRequest firstRunPicker;
    private final Completion firstRunCompletion;
    private AlertDialog folderDialog;
    private Runnable firstRunOffer;
    private boolean resumed;
    private Runnable stopObservingSources = () -> { };
    private Supplier<Boolean> customPickerPending = () -> false;
    private Runnable onFolderPromptClosed = () -> { };

    public AndroidBRouterStorageSetup(Activity activity, @Nullable Bundle state) {
        this.activity = activity;
        requests = new AndroidBRouterFolderRequests(activity);
        foreground = new AndroidBRouterForegroundChecks(activity, this, requests, state);
        firstRunPicker = new AndroidBRouterPickerRequest(activity, STATE_FIRST_RUN);
        firstRunCompletion = firstRunPicker.restore(state);
        if (firstRunCompletion != null) {
            finishFirstRunOffer(firstRunCompletion);
        }
    }

    public boolean completedFirstRunStep() {
        return firstRunCompletion != null && firstRunCompletion != Completion.RETRY;
    }

    public static boolean hasSharedPickerState(Bundle state) {
        return state.containsKey(STATE_FIRST_RUN);
    }

    public void watchFirstRun(Runnable onAdvance, Runnable onRetry) {
        firstRunPicker.watch(completion -> onFirstRunResult(completion, onAdvance, onRetry));
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
            requests.findLocation(foreground.offers().copy(), AndroidBRouterPickerLocation.browse(),
                    location -> {
                        firstRunOffer = () -> showFirstRunFolder(location, onCancel);
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

    public void bindManualAction(View action, Runnable onPickerFinished) {
        foreground.bindAction(action, () -> {
            onPickerFinished.run();
            onFolderPromptClosed.run();
        });
        stopObservingSources = BRouterFolderGrant.observe(activity, foreground::sourcesChanged);
    }

    public void coordinatePicker(Supplier<Boolean> customPickerPending, Runnable onFolderPromptClosed) {
        this.customPickerPending = customPickerPending;
        this.onFolderPromptClosed = onFolderPromptClosed;
    }

    public boolean isPickerPending() {
        return firstRunPicker.isPending() || foreground.hasActivePicker();
    }

    public void setForegroundChecksEnabled(boolean enabled) {
        foreground.setEnabled(enabled);
    }

    public ArrayList<String> skippedSegmentsFolders() {
        return foreground.offers().copy();
    }

    void showFolder(AndroidBRouterPickerLocation location, Runnable beforeLaunch, Runnable onCancel, Runnable onFailure) {
        folderDialog = AndroidBRouterFolderPrompt.show(activity, location.initialUri, REQUEST_FOLDER, TAG,
                beforeLaunch, onCancel, onFailure);
        refreshAfterDismiss(() -> { });
    }

    private void showFirstRunFolder(AndroidBRouterPickerLocation location, Runnable onCancel) {
        if (!requests.needsFolderAccess() || !requests.needsGrant()) {
            onCancel.run();
            return;
        }
        showFolder(location, () -> {
            foreground.offers().begin(location.confirmedUri());
            firstRunPicker.begin(REQUEST_FOLDER);
        }, () -> {
            foreground.offers().record(location.confirmedUri());
            onCancel.run();
        }, () -> {
            foreground.deferAutomaticOffer();
            onCancel.run();
        });
    }

    public void acceptLegacyPickerResult(boolean profiles, int resultCode, @Nullable Intent data) {
        AndroidBRouterFolderRequests.acceptLegacyPickerResult(activity, profiles, resultCode, data);
    }

    public boolean handleActivityResult(
            int requestCode, int resultCode, @Nullable Intent data, Runnable onAdvance, Runnable onRetry
    ) {
        if (firstRunPicker.matches(requestCode)) {
            requests.handleResult(resultCode, data, firstRunPicker,
                    completion -> onFirstRunResult(completion, onAdvance, onRetry));
            return true;
        }
        return foreground.handleResult(requestCode, resultCode, data);
    }

    private void onFirstRunResult(Completion completion, Runnable onAdvance, Runnable onRetry) {
        finishFirstRunOffer(completion);
        if (completion == Completion.RETRY) {
            onRetry.run();
        } else {
            onAdvance.run();
        }
        onFolderPromptClosed.run();
    }

    private void finishFirstRunOffer(Completion completion) {
        var offers = foreground.offers();
        offers.finish(completion == Completion.RETRY ? Completion.CANCELED : completion, false);
    }

    boolean canShowDialog() {
        return !activity.isFinishing() && !activity.isDestroyed() && !customPickerPending.get();
    }

    public void saveState(Bundle state) {
        firstRunPicker.save(state);
        foreground.saveState(state);
    }

    public void dispose() {
        stopObservingSources.run();
        foreground.dispose();
        firstRunPicker.stopWatching();
        if (isDialogShowing()) {
            folderDialog.dismiss();
        }
    }

    boolean isDialogShowing() {
        return folderDialog != null && folderDialog.isShowing();
    }

    void refreshAfterDismiss(Runnable refresh) {
        folderDialog.setOnDismissListener(dialog -> {
            onFolderPromptClosed.run();
            refresh.run();
        });
    }
}
