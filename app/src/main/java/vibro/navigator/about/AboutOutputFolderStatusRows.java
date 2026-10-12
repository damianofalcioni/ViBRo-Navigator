package vibro.navigator.about;

import android.app.Activity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.storage.AndroidOutputStorageAccess;

/** Shared legacy output permission is inspected off the UI thread, even when saving is disabled. */
final class AboutOutputFolderStatusRows {
    private final Activity activity;
    private final ExecutorService worker;
    private final AboutPermissionRow row;
    private boolean checking;
    private int generation;

    AboutOutputFolderStatusRows(Activity activity) {
        this(activity, Executors.newSingleThreadExecutor());
    }

    AboutOutputFolderStatusRows(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
        row = new AboutPermissionRow(activity, R.id.aboutPermissionOutputFolderRow,
                R.id.aboutPermissionOutputFolderMark, R.id.aboutPermissionOutputFolderLabel,
                R.id.aboutPermissionOutputFolderStatus, () -> AboutOutputFolderSettings.requestAccess(activity));
    }

    void render() {
        boolean visible = AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant();
        row.setVisible(visible);
        if (visible && !checking) {
            checking = true;
            int requestGeneration = generation;
            worker.execute(() -> inspect(requestGeneration));
        }
    }

    private void inspect(int requestGeneration) {
        boolean allowed = AndroidOutputStorageAccess.hasRequiredPermission(activity);
        activity.runOnUiThread(() -> apply(requestGeneration, allowed));
    }

    private void apply(int requestGeneration, boolean allowed) {
        if (requestGeneration != generation || activity.isDestroyed()) {
            return;
        }
        checking = false;
        row.render(allowed);
    }

    void stop() {
        generation++;
        checking = false;
    }

    void shutdown() {
        stop();
        worker.shutdown();
    }
}
