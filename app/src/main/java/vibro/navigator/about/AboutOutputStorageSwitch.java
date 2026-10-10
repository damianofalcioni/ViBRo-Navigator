package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.android.storage.AndroidOutputStorageAccess;
import vibro.navigator.android.storage.AndroidOutputStorageVolumes;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppOutputStorageSettings;

final class AboutOutputStorageSwitch {
    static final int REQUEST_SD_FOLDER = 4012;
    private final Activity activity;
    private final ExecutorService worker;
    private final Switch switchView;
    private final AboutDeferredBooleanSetting setting;
    private boolean checking;

    AboutOutputStorageSwitch(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
        switchView = activity.findViewById(R.id.aboutExternalStorageSwitch);
        setting = new AboutDeferredBooleanSetting(AndroidTaskScheduler.main(), this::change, this::render);
    }

    void configure() {
        render();
        switchView.setOnCheckedChangeListener((button, enabled) -> setting.set(enabled));
    }

    void render() {
        switchView.setEnabled(!checking && (AppOutputStorageSettings.useSdCard(activity)
                || AndroidOutputStorageVolumes.find(activity, "") != null));
        setting.render(switchView, AppOutputStorageSettings.useSdCard(activity));
    }

    private void change(boolean enabled) {
        if (!enabled) {
            AppOutputStorageSettings.disable(activity);
            render();
            worker.execute(() -> {
                AndroidOutputStorage.resetFailures();
                AppLogger.refreshOutputFolder(activity.getApplicationContext());
            });
            return;
        }
        render();
        requestAccess();
    }

    void requestAccess() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(activity, "");
        if (card == null) {
            toast(R.string.msg_sd_storage_unavailable);
            return;
        }
        if (AndroidOutputStorageAccess.needsCardGrant(activity, card)) {
            prompt(card);
        } else {
            check(() -> AndroidOutputStorageAccess.enable(activity, card,
                    AndroidOutputStorageAccess.existingGrant(activity, card)));
        }
    }

    private void prompt(AndroidOutputStorageVolumes.Card card) {
        new AlertDialog.Builder(activity).setTitle(R.string.label_external_storage_enabled)
                .setMessage(activity.getString(R.string.hint_sd_download_access, card.root + "/Download"))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> launch(card.id)).show();
    }

    private void launch(String id) {
        try {
            activity.startActivityForResult(AndroidOutputStorageAccess.picker(id), REQUEST_SD_FOLDER);
        } catch (ActivityNotFoundException e) {
            toast(R.string.msg_output_folder_picker_unavailable);
        }
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_SD_FOLDER) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null) {
            check(() -> AndroidOutputStorageAccess.accept(activity, data));
        }
        render();
        return true;
    }

    private void check(AccessAction action) {
        checking = true;
        render();
        worker.execute(() -> {
            int message;
            try {
                action.run();
                AppLogger.refreshOutputFolder(activity.getApplicationContext());
                message = R.string.msg_sd_storage_enabled;
            } catch (IOException | RuntimeException e) {
                message = R.string.msg_output_folder_failed;
            }
            int result = message;
            activity.runOnUiThread(() -> {
                checking = false;
                if (!activity.isDestroyed()) {
                    render();
                    toast(result);
                }
            });
        });
    }

    private void toast(int message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    void flush() {
        setting.flush(false);
    }

    private interface AccessAction {
        void run() throws IOException;
    }
}
