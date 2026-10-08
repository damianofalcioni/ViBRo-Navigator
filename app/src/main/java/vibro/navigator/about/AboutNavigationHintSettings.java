package vibro.navigator.about;

import android.app.Activity;
import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.settings.AppNavigationHintSettings;

/** Owns the hint preference and its deferred persistence across settings exits/imports. */
final class AboutNavigationHintSettings {
    private final Activity activity;
    private final Switch switchView;
    private final AboutDeferredBooleanSetting setting;

    AboutNavigationHintSettings(@NonNull Activity activity) {
        this.activity = activity;
        switchView = activity.findViewById(R.id.aboutShowHintPanelSwitch);
        setting = new AboutDeferredBooleanSetting(AndroidTaskScheduler.main(),
                enabled -> AppNavigationHintSettings.setEnabled(activity, enabled), () -> { });
        refresh();
        switchView.setOnCheckedChangeListener((button, checked) -> setting.set(checked));
    }

    void refresh() {
        setting.render(switchView, AppNavigationHintSettings.isEnabled(activity));
    }

    void flush() {
        setting.flush(false);
    }
}
