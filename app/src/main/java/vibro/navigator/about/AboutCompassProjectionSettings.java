package vibro.navigator.about;

import android.app.Activity;
import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.settings.AppCompassSettings;

/** Owns the deferred projection preference and its import/lifecycle refresh. */
final class AboutCompassProjectionSettings {
    private final Activity activity;
    private final Switch switchView;
    private final AboutDeferredBooleanSetting setting;

    AboutCompassProjectionSettings(@NonNull Activity activity) {
        this.activity = activity;
        switchView = activity.findViewById(R.id.aboutCompassCentralPerspectiveSwitch);
        setting = new AboutDeferredBooleanSetting(AndroidTaskScheduler.main(),
                enabled -> AppCompassSettings.setCentralPerspectiveEnabled(activity, enabled), () -> { });
        refresh();
        switchView.setOnCheckedChangeListener((button, checked) -> setting.set(checked));
    }

    void refresh() {
        setting.render(switchView, AppCompassSettings.isCentralPerspectiveEnabled(activity));
    }

    void flush() {
        setting.flush(false);
    }
}
