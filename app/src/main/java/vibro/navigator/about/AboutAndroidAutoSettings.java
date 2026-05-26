package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.settings.AppAndroidAutoSettings;

final class AboutAndroidAutoSettings {
    private final Activity activity;
    private final Switch enabledSwitch;
    private boolean rendering;

    AboutAndroidAutoSettings(@NonNull Activity activity, @NonNull Switch enabledSwitch) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
    }

    void configure() {
        refresh();
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rendering) {
                return;
            }
            if (!DistributionServices.supportsAndroidAutoIntegration()) {
                refresh();
                return;
            }
            AppAndroidAutoSettings.setIntegrationEnabled(activity, isChecked);
            DistributionServices.configureAndroidAutoIntegration(activity, isChecked);
        });
    }

    void refresh() {
        boolean supported = DistributionServices.supportsAndroidAutoIntegration();
        boolean enabled = supported && AppAndroidAutoSettings.isIntegrationEnabled(activity);
        rendering = true;
        enabledSwitch.setVisibility(supported ? View.VISIBLE : View.GONE);
        enabledSwitch.setEnabled(supported);
        enabledSwitch.setChecked(enabled);
        rendering = false;
        if (supported) {
            DistributionServices.configureAndroidAutoIntegration(activity, enabled);
        }
    }
}
