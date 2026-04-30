package vibro.navigator;

import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.brouter.BRouterInstallLauncher;

final class MainActivityBRouterInstallPrompt {

    private MainActivityBRouterInstallPrompt() {
    }

    static void show(@NonNull MainActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_not_found)
                .setMessage(R.string.msg_brouter_install_prompt)
                .setPositiveButton(R.string.action_open_play_store, (dialog, which) -> {
                    if (!BRouterInstallLauncher.launchPlayStore(activity)) {
                        Toast.makeText(activity, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton(R.string.action_open_fdroid, (dialog, which) -> {
                    if (!BRouterInstallLauncher.launchFdroid(activity)) {
                        Toast.makeText(activity, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
