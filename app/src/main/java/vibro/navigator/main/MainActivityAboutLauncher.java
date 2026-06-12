package vibro.navigator.main;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import vibro.navigator.about.AboutActivity;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class MainActivityAboutLauncher {

    private static final String TAG = "MainActivity";
    private static final long ABOUT_LAUNCH_DELAY_MS = 100L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;

    private MainActivityAboutLauncher(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
    }

    static void configure(@NonNull Activity activity, @NonNull View aboutButton) {
        MainActivityAboutLauncher launcher = new MainActivityAboutLauncher(
                activity,
                AndroidTaskScheduler.main()
        );
        aboutButton.setOnClickListener(v -> launcher.launchAfterPressFeedback());
    }

    private void launchAfterPressFeedback() {
        AppLogger.i(TAG, "About button tapped");
        scheduler.postDelayed(
                () -> activity.startActivity(new Intent(activity, AboutActivity.class)),
                ABOUT_LAUNCH_DELAY_MS
        );
    }
}
