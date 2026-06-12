package vibro.navigator.about;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;

final class AboutDeferredDialogAction {

    // Lets button press feedback draw before constructing and showing a settings dialog.
    static final long OPEN_DELAY_MS = 100L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Runnable openDialog;
    @NonNull
    private final Runnable deferredOpen;

    private boolean scheduled;

    AboutDeferredDialogAction(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable openDialog
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
        this.openDialog = openDialog;
        deferredOpen = this::openIfActive;
    }

    static void configure(
            @NonNull Activity activity,
            @NonNull View button,
            @NonNull Runnable openDialog
    ) {
        new AboutDeferredDialogAction(
                activity,
                AndroidTaskScheduler.main(),
                openDialog
        ).attachTo(button);
    }

    void attachTo(@NonNull View button) {
        button.setOnClickListener(v -> scheduleOpen());
    }

    private void scheduleOpen() {
        if (scheduled) {
            return;
        }
        scheduled = true;
        scheduler.postDelayed(deferredOpen, OPEN_DELAY_MS);
    }

    private void openIfActive() {
        scheduled = false;
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        openDialog.run();
    }
}
