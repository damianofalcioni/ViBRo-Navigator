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
    private static final String STATE_ENABLE_REQUESTED = "about_streets_enable_requested";
    private final Activity activity;
    private final AndroidBRouterPickerRequest picker;
    private boolean enableRequested;
    @Nullable
    private Boolean restoredResult;
    @Nullable
    private Consumer<Boolean> onResult;

    AboutStreetPicker(Activity activity) {
        this.activity = activity;
        picker = new AndroidBRouterPickerRequest(activity, "about_streets_picker_pending");
    }

    void restore(@Nullable Bundle state) {
        enableRequested = state != null && state.getBoolean(STATE_ENABLE_REQUESTED, true);
        acceptCompletion(picker.restore(state));
        picker.watch(this::acceptCompletion);
    }

    private void acceptCompletion(@Nullable AndroidBRouterPickerRequest.Completion completion) {
        if (completion != null && enableRequested) {
            enableRequested = false;
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
        enableRequested = true;
        if (!picker.isPending()) {
            AndroidBRouterSegmentsTreeAccessPrompt.show(activity, REQUEST, logTag, () -> picker.begin(REQUEST), () -> {
                if (enableRequested) {
                    enableRequested = false;
                    onCancel.run();
                }
            });
        }
    }

    void changeSetting(boolean enabled, AboutDeferredBooleanSetting setting, Runnable requestEnable) {
        if (setting.isRendering()) {
            return;
        }
        if (enabled) {
            requestEnable.run();
        } else {
            enableRequested = false;
            setting.set(false);
        }
    }

    boolean displayedValue(boolean storedEnabled) {
        return storedEnabled || (enableRequested && picker.isPending());
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
        if (!activity.isDestroyed() && enableRequested) {
            enableRequested = false;
            restoredResult = saved;
            deliver();
        }
    }

    void save(Bundle state) {
        picker.save(state);
        state.putBoolean(STATE_ENABLE_REQUESTED, enableRequested);
    }

    void dispose() {
        picker.stopWatching();
    }
}
