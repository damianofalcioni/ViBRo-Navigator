package vibro.navigator.about;

import android.app.Activity;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Legacy folder/grant checks stay off the UI thread, including when saving is disabled. */
final class AboutOutputFolderStatusRows {
    private final Activity activity;
    private final ExecutorService worker;
    private final Map<Kind, AboutPermissionRow> rows = new EnumMap<>(Kind.class);
    private boolean checking;
    private int generation;

    AboutOutputFolderStatusRows(Activity activity) {
        this(activity, Executors.newSingleThreadExecutor());
    }

    AboutOutputFolderStatusRows(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
        rows.put(Kind.LOGS, new AboutPermissionRow(activity, R.id.aboutPermissionLogFolderRow,
                R.id.aboutPermissionLogFolderMark, R.id.aboutPermissionLogFolderLabel,
                R.id.aboutPermissionLogFolderStatus, () -> AboutOutputFolderSettings.requestAccess(activity)));
        rows.put(Kind.GPX, new AboutPermissionRow(activity, R.id.aboutPermissionGpxFolderRow,
                R.id.aboutPermissionGpxFolderMark, R.id.aboutPermissionGpxFolderLabel,
                R.id.aboutPermissionGpxFolderStatus, () -> AboutOutputFolderSettings.requestAccess(activity)));
    }

    void render() {
        boolean visible = AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant();
        for (AboutPermissionRow row : rows.values()) {
            row.setVisible(visible);
        }
        if (visible && !checking) {
            checking = true;
            int requestGeneration = generation;
            worker.execute(() -> inspect(requestGeneration));
        }
    }

    private void inspect(int requestGeneration) {
        Map<Kind, Boolean> allowed = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            allowed.put(kind, AndroidOutputStorage.isUsable(activity, kind));
        }
        activity.runOnUiThread(() -> apply(requestGeneration, allowed));
    }

    private void apply(int requestGeneration, Map<Kind, Boolean> allowed) {
        if (requestGeneration != generation || activity.isDestroyed()) {
            return;
        }
        checking = false;
        for (Kind kind : Kind.values()) {
            rows.get(kind).render(Boolean.TRUE.equals(allowed.get(kind)));
        }
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
