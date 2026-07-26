package vibro.navigator.nav.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetResources;

final class NavigationCustomButtonUi {
    @NonNull
    private final Activity activity;
    @NonNull
    private final ImageButton button;
    @NonNull
    private final NavigationCustomButtonController controller;

    NavigationCustomButtonUi(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull NavigationCustomButtonController.Host host
    ) {
        this.activity = activity;
        button = activity.findViewById(R.id.navigationCustomButton);
        controller = new NavigationCustomButtonController(activity, uiScheduler, host);
    }

    void configure() {
        button.setOnClickListener(v -> controller.toggleSelectedSetting());
    }

    void render() {
        if (!AppNavigationCustomButtonSettings.isEnabled(activity)) {
            button.setVisibility(View.GONE);
            return;
        }
        Target target = AppNavigationCustomButtonSettings.getTarget(activity);
        boolean settingEnabled = NavigationCustomButtonSettingState.isEnabled(activity, target);
        button.setImageResource(AppNavigationCustomButtonTargetResources.iconResId(target, settingEnabled));
        button.setContentDescription(activity.getString(
                R.string.format_action_toggle_custom_button_setting,
                activity.getString(AppNavigationCustomButtonTargetResources.labelResId(target)),
                activity.getString(settingEnabled ? R.string.label_setting_enabled : R.string.label_setting_disabled)
        ));
        button.setVisibility(View.VISIBLE);
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        return controller.onRequestPermissionsResult(requestCode, grantResults);
    }
}
