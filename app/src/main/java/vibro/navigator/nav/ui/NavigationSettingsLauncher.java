package vibro.navigator.nav.ui;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

import vibro.navigator.about.AboutActivity;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class NavigationSettingsLauncher {
    private static final String TAG = "NavigationActivity";
    private static final long SETTINGS_LAUNCH_DELAY_MS = 100L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Runnable afterLaunch;
    @NonNull
    private final Runnable launchSettings = this::launchSettings;

    private NavigationSettingsLauncher(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable afterLaunch
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
        this.afterLaunch = afterLaunch;
    }

    static void configure(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler,
            @NonNull View settingsButton,
            @NonNull Runnable afterLaunch
    ) {
        NavigationSettingsLauncher launcher = new NavigationSettingsLauncher(activity, scheduler, afterLaunch);
        settingsButton.setOnClickListener(v -> launcher.launchAfterPressFeedback());
    }

    private void launchAfterPressFeedback() {
        AppLogger.i(TAG, "Navigation settings button tapped");
        scheduler.removeCallbacks(launchSettings);
        scheduler.postDelayed(launchSettings, SETTINGS_LAUNCH_DELAY_MS);
    }

    private void launchSettings() {
        afterLaunch.run();
        activity.startActivity(AboutActivity.settingsIntent(activity));
    }
}
