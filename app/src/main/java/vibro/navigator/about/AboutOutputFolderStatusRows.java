package vibro.navigator.about;

import android.app.Activity;
import android.widget.TextView;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputFolderAccess;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Provider checks run off the UI thread and never change the selected folder. */
final class AboutOutputFolderStatusRows {
    private final Activity activity;
    private final ExecutorService worker;
    private final Map<Kind, AboutPermissionRow> rows = new EnumMap<>(Kind.class);
    private final Map<Kind, String> checked = new EnumMap<>(Kind.class);
    private final Map<Kind, Boolean> allowed = new EnumMap<>(Kind.class);
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
                R.id.aboutPermissionLogFolderStatus, () -> AboutOutputFolderSettings.requestAccess(activity, Kind.LOGS)));
        rows.put(Kind.GPX, new AboutPermissionRow(activity, R.id.aboutPermissionGpxFolderRow,
                R.id.aboutPermissionGpxFolderMark, R.id.aboutPermissionGpxFolderLabel,
                R.id.aboutPermissionGpxFolderStatus, () -> AboutOutputFolderSettings.requestAccess(activity, Kind.GPX)));
    }

    void render() {
        Map<Kind, String> selected = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            String uri = AppOutputFolderSettings.get(activity, kind);
            rows.get(kind).setVisible(uri != null);
            if (uri != null) {
                selected.put(kind, uri);
                renderLabel(kind);
                rows.get(kind).render(Objects.equals(uri, checked.get(kind)) && Boolean.TRUE.equals(allowed.get(kind)));
            }
        }
        if (!checking && !selected.isEmpty()) {
            checking = true;
            int requestGeneration = generation;
            worker.execute(() -> inspect(selected, requestGeneration));
        }
    }

    private void renderLabel(Kind kind) {
        TextView label = activity.findViewById(kind == Kind.LOGS
                ? R.id.aboutPermissionLogFolderLabel : R.id.aboutPermissionGpxFolderLabel);
        label.setText(activity.getString(R.string.format_output_folder_diagnostic,
                activity.getString(kind == Kind.LOGS ? R.string.title_log_output_folder
                        : R.string.title_gpx_output_folder), AndroidOutputFolderAccess.label(activity, kind)));
    }

    private void inspect(Map<Kind, String> selected, int requestGeneration) {
        Map<Kind, Boolean> result = new EnumMap<>(Kind.class);
        for (Kind kind : selected.keySet()) {
            result.put(kind, AndroidOutputFolderAccess.isUsable(activity.getApplicationContext(), kind));
        }
        activity.runOnUiThread(() -> apply(selected, result, requestGeneration));
    }

    private void apply(Map<Kind, String> selected, Map<Kind, Boolean> result, int requestGeneration) {
        if (requestGeneration != generation || activity.isDestroyed()) {
            return;
        }
        checking = false;
        for (Kind kind : selected.keySet()) {
            if (Objects.equals(selected.get(kind), AppOutputFolderSettings.get(activity, kind))) {
                checked.put(kind, selected.get(kind));
                allowed.put(kind, result.get(kind));
                rows.get(kind).render(Boolean.TRUE.equals(result.get(kind)));
            }
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
