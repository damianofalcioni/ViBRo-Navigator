package vibro.navigator.main;

import vibro.navigator.R;

import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.brouter.AndroidBRouterInstallLauncher;

final class MainActivityBRouterInstallPrompt {

    private MainActivityBRouterInstallPrompt() {
    }

    static AlertDialog show(@NonNull MainActivity activity) {
        return show(activity, null, () -> { }, () -> { });
    }

    static AlertDialog show(@NonNull MainActivity activity, @Nullable Integer requestCode,
                            @NonNull Runnable onLaunch, @NonNull Runnable onSkipped) {
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.title_brouter_not_found)
                .setMessage(R.string.msg_brouter_install_prompt)
                .setPositiveButton(R.string.action_open_play_store, (dialog, which) -> {
                    onLaunch.run();
                    if (!AndroidBRouterInstallLauncher.launchPlayStore(activity, requestCode)) {
                        Toast.makeText(activity, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                        onSkipped.run();
                    }
                })
                .setNeutralButton(R.string.action_open_fdroid, (dialog, which) -> {
                    onLaunch.run();
                    if (!AndroidBRouterInstallLauncher.launchFdroid(activity, requestCode)) {
                        Toast.makeText(activity, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                        onSkipped.run();
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> onSkipped.run())
                .setOnCancelListener(dialog -> onSkipped.run())
                .show();
    }
}

