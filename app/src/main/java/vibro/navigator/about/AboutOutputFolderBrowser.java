package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputFolderBrowser;
import vibro.navigator.android.storage.AndroidOutputFolderBrowser.Entry;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

final class AboutOutputFolderBrowser {
    private final Activity activity;
    private final ExecutorService worker;

    AboutOutputFolderBrowser(Activity activity, ExecutorService worker) {
        this.activity = activity;
        this.worker = worker;
    }

    void open(Kind kind) {
        String selected = AppOutputFolderSettings.get(activity, kind);
        worker.execute(() -> load(kind, selected));
    }

    private void load(Kind kind, String selected) {
        try {
            if (selected == null) {
                List<Entry> files = AndroidOutputFolderBrowser.defaultFiles(activity.getApplicationContext(), kind);
                onUi(() -> showFiles(kind, files));
            } else {
                Intent intent = AndroidOutputFolderBrowser.customFolder(activity.getApplicationContext(), selected);
                onUi(() -> launch(intent));
            }
        } catch (IOException | RuntimeException e) {
            onUi(() -> toast(R.string.msg_output_folder_open_failed));
        }
    }

    private void showFiles(Kind kind, List<Entry> files) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                .setTitle(kind == Kind.LOGS ? R.string.title_log_output_folder : R.string.title_gpx_output_folder)
                .setNegativeButton(android.R.string.cancel, null);
        if (files.isEmpty()) {
            dialog.setMessage(R.string.msg_output_folder_empty);
        } else {
            String[] names = new String[files.size()];
            for (int i = 0; i < files.size(); i++) {
                names[i] = files.get(i).name;
            }
            dialog.setItems(names, (ignored, index) -> launch(files.get(index).intent));
        }
        dialog.show();
    }

    private void launch(Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(R.string.msg_output_viewer_unavailable);
        } catch (SecurityException e) {
            toast(R.string.msg_output_folder_open_failed);
        }
    }

    private void onUi(Runnable action) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                action.run();
            }
        });
    }

    private void toast(int message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
