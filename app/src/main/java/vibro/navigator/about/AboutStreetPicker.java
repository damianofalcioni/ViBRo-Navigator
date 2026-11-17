package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterPickerRequest;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;

final class AboutStreetPicker {
    private static final int REQUEST = AboutSettingsSwitches.REQUEST_SURROUNDING_STREETS_SEGMENTS_TREE;
    private final Activity activity;
    private final AndroidBRouterPickerRequest picker;
    @Nullable
    private Boolean restoredResult;
    @Nullable
    private Consumer<Boolean> onResult;

    AboutStreetPicker(Activity activity) {
        this.activity = activity;
        picker = new AndroidBRouterPickerRequest(activity, "about_streets_picker_pending");
    }

    void restore(@Nullable Bundle state) {
        acceptCompletion(picker.restore(state));
        picker.watch(this::acceptCompletion);
    }

    private void acceptCompletion(@Nullable AndroidBRouterPickerRequest.Completion completion) {
        if (completion != null) {
            restoredResult = completion == AndroidBRouterPickerRequest.Completion.ADVANCE;
            deliver();
        }
    }

    void bind(Consumer<Boolean> onResult) {
        this.onResult = onResult;
        deliver();
    }

    private void deliver() {
        if (onResult != null && restoredResult != null) {
            boolean saved = restoredResult;
            restoredResult = null;
            onResult.accept(saved);
        }
    }

    void request(String logTag, Runnable onCancel) {
        if (!picker.isPending()) {
            AndroidBRouterSegmentsTreeAccessPrompt.show(activity, REQUEST, logTag, () -> picker.begin(REQUEST), onCancel);
        }
    }

    boolean handleResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST) {
            return false;
        }
        if (picker.matches(requestCode)) {
            completeResult(resultCode, data);
        }
        return true;
    }

    private void completeResult(int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            onFolderSaved(false);
            return;
        }
        picker.beginValidation();
        AndroidBRouterFolderAccess.saveSelection(activity, data, this::onFolderSaved);
    }

    private void onFolderSaved(boolean saved) {
        picker.complete(saved ? AndroidBRouterPickerRequest.Completion.ADVANCE : AndroidBRouterPickerRequest.Completion.RETRY);
        if (!activity.isDestroyed()) {
            restoredResult = saved;
            deliver();
        }
    }

    void save(Bundle state) {
        picker.save(state);
    }

    void dispose() {
        picker.stopWatching();
    }
}
