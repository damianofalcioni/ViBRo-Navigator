package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.settings.AppAndroidAutoSettings;

final class AboutAndroidAutoSettings {
    private final Activity activity;
    private final Switch enabledSwitch;
    private final AboutDeferredBooleanSetting enabledSetting;
    private boolean rendering;

    AboutAndroidAutoSettings(@NonNull Activity activity, @NonNull Switch enabledSwitch) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
        enabledSetting = new AboutDeferredBooleanSetting(
                AndroidTaskScheduler.main(),
                this::applyEnabled,
                () -> {
                }
        );
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
            enabledSetting.set(isChecked);
        });
    }

    void refresh() {
        boolean supported = DistributionServices.supportsAndroidAutoIntegration();
        boolean enabled = supported && AppAndroidAutoSettings.isIntegrationEnabled(activity);
        rendering = true;
        enabledSwitch.setVisibility(supported ? View.VISIBLE : View.GONE);
        enabledSwitch.setEnabled(supported);
        enabledSetting.render(enabledSwitch, enabled);
        rendering = false;
        if (supported) {
            DistributionServices.configureAndroidAutoIntegration(activity, enabled);
        }
    }

    void flush() {
        enabledSetting.flush();
    }

    private void applyEnabled(boolean enabled) {
        AppAndroidAutoSettings.setIntegrationEnabled(activity, enabled);
        DistributionServices.configureAndroidAutoIntegration(activity, enabled);
    }
}
