package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidOutputStorage;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.settings.AppAndroidAutoSettings;
import vibro.navigator.settings.AppSettingsDefaults;

final class AboutRestoreDefaultsAction {
    private AboutRestoreDefaultsAction() {
    }

    static void configure(Activity activity, Runnable beforeRestore) {
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutRestoreDefaultsButton), () ->
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.action_restore_default_configuration)
                                .setMessage(R.string.confirm_restore_default_configuration)
                                .setNegativeButton(R.string.action_cancel, null)
                                .setPositiveButton(R.string.action_restore_defaults, (dialog, which) -> {
                                    beforeRestore.run();
                                    restore(activity);
                                })
                                .show());
    }

    private static void restore(Activity activity) {
        AppSettingsDefaults.restore(activity);
        AndroidOutputStorage.resetFailures();
        if (DistributionServices.supportsAndroidAutoIntegration()) {
            DistributionServices.configureAndroidAutoIntegration(activity,
                    AppAndroidAutoSettings.isIntegrationEnabled(activity));
        }
        Toast.makeText(activity, R.string.msg_default_configuration_restored, Toast.LENGTH_SHORT).show();
        // Old switch states must not be restored over the defaults during recreation.
        activity.findViewById(R.id.aboutRoot).setSaveFromParentEnabled(false);
        activity.recreate();
    }
}
