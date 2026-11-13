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
    static final int REQUEST_PROFILES = 1102;
    static final int REQUEST_SEGMENTS = 1103;
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

    public boolean requestProfiles(Runnable onCancel) {
        return requestForFirstRun(REQUEST_PROFILES, onCancel);
    }

    public boolean requestSegments(Runnable onCancel) {
        return requestForFirstRun(REQUEST_SEGMENTS, onCancel);
    }

    public boolean completedFirstRunStep() {
        return firstRunCompletion == Completion.ADVANCE;
    }

    private boolean requestForFirstRun(int requestCode, Runnable onCancel) {
        if (firstRunPicker.matches(requestCode)) {
            return true;
        }
        if (!requests.needsFolderAccess() || !requests.needsGrant(requestCode)) {
            return false;
        }
        AndroidTaskScheduler.main().postDelayed(() -> discoverFirstRun(requestCode, onCancel),
                ViewConfiguration.getPressedStateDuration());
        return true;
    }

    private void discoverFirstRun(int requestCode, Runnable onCancel) {
        if (canShowDialog()) {
            requests.findLocation(requestCode, Collections.emptySet(), AndroidBRouterPickerLocation.browse(),
                    location -> {
                        firstRunOffer = () -> showFolder(requestCode, location,
                                () -> firstRunPicker.begin(requestCode), onCancel);
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

    public void bindManualActions(View profilesAction, View segmentsAction) {
        foreground.bindActions(profilesAction, segmentsAction);
    }

    public void setForegroundChecksEnabled(boolean enabled) {
        foreground.setEnabled(enabled);
    }

    public ArrayList<String> skippedSegmentsFolders() {
        return foreground.offers().copy(REQUEST_SEGMENTS);
    }

    void showFolder(int requestCode, AndroidBRouterPickerLocation location, Runnable beforeLaunch, Runnable onCancel) {
        Runnable launch = rememberOffer(requestCode, location, beforeLaunch);
        Runnable cancel = rememberOffer(requestCode, location, onCancel);
        folderDialog = requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.showNow(activity, location.initialUri, requestCode, TAG,
                        launch, cancel, cancel)
                : AndroidBRouterSegmentsTreeAccessPrompt.showNow(activity, location.initialUri, requestCode, TAG,
                        launch, cancel, cancel);
    }

    private Runnable rememberOffer(int requestCode, AndroidBRouterPickerLocation location, Runnable callback) {
        return () -> {
            foreground.offers().record(requestCode, location.confirmedUri());
            callback.run();
        };
    }

    public boolean handleActivityResult(
            int requestCode, int resultCode, @Nullable Intent data, Runnable onAdvance, Runnable onRetry
    ) {
        if (firstRunPicker.matches(requestCode)) {
            requests.handleResult(requestCode, resultCode, data, firstRunPicker, onAdvance, onRetry);
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
