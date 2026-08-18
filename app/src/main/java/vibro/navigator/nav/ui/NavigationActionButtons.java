package vibro.navigator.nav.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.service.NavigationServiceBinder;

final class NavigationActionButtons {
    interface Listener {
        void onStopNavigation();

        void onTogglePaused();

        void onExportRoute();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler uiScheduler;
    @NonNull
    private final Runnable afterSettingsLaunch;
    @NonNull
    private final ImageButton export;
    @NonNull
    private final ImageButton settings;
    @NonNull
    private final ImageButton pauseResume;
    @NonNull
    private final ImageButton stop;
    @Nullable
    private Boolean lastBinderAvailable;
    @Nullable
    private Boolean lastPaused;

    NavigationActionButtons(
            @NonNull Activity activity,
            @NonNull TaskScheduler uiScheduler,
            @NonNull Runnable afterSettingsLaunch
    ) {
        this.activity = activity;
        this.uiScheduler = uiScheduler;
        this.afterSettingsLaunch = afterSettingsLaunch;
        export = activity.findViewById(R.id.exportRouteButton);
        settings = activity.findViewById(R.id.navigationSettingsButton);
        pauseResume = activity.findViewById(R.id.pauseResumeNavButton);
        stop = activity.findViewById(R.id.stopNavButton);
    }

    void configure(@NonNull Listener listener) {
        export.setOnClickListener(v -> listener.onExportRoute());
        NavigationSettingsLauncher.configure(activity, uiScheduler, settings, afterSettingsLaunch);
        stop.setOnClickListener(v -> listener.onStopNavigation());
        pauseResume.setOnClickListener(v -> listener.onTogglePaused());
    }

    void render(@NonNull NavState state, @Nullable NavigationServiceBinder navBinder) {
        boolean binderAvailable = navBinder != null;
        if (lastBinderAvailable == null || lastBinderAvailable != binderAvailable) {
            lastBinderAvailable = binderAvailable;
            export.setEnabled(binderAvailable);
            pauseResume.setEnabled(binderAvailable);
        }
        boolean paused = state.pauseStatus.paused;
        if (lastPaused == null || lastPaused != paused) {
            lastPaused = paused;
            pauseResume.setImageResource(paused ? R.drawable.ic_play : R.drawable.ic_pause);
            pauseResume.setContentDescription(activity.getString(
                    paused ? R.string.action_resume_navigation : R.string.action_pause_navigation
            ));
        }
    }

    @NonNull
    View settingsAnchor() {
        return settings;
    }

    @NonNull
    View exportAnchor() {
        return export;
    }
}
