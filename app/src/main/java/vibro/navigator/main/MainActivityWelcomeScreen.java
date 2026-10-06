package vibro.navigator.main;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.settings.AppMainUiSettings;
import vibro.navigator.settings.AppThemeSettings;

/** Keeps first-launch guidance ahead of setup prompts and incoming route actions. */
final class MainActivityWelcomeScreen {
    private static final String STATE_WELCOME = "main_welcome";
    // Allow the Continue button's press feedback to draw before saving and opening setup.
    private static final long CONTINUE_DELAY_MS = 100L;

    private final Dialog dialog;
    private final TaskScheduler scheduler = AndroidTaskScheduler.main();
    private final Runnable continueSetup;

    private MainActivityWelcomeScreen(@NonNull Activity activity, @NonNull Runnable onContinue) {
        dialog = new Dialog(activity, AppThemeSettings.isLightThemeEnabled(activity)
                ? R.style.Theme_ViBRoNavigator_Light : R.style.Theme_ViBRoNavigator);
        dialog.setContentView(R.layout.screen_welcome);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(ignored -> activity.finish());
        continueSetup = () -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            AppMainUiSettings.completeWelcome(activity);
            dialog.dismiss();
            onContinue.run();
        };
        View continueButton = dialog.findViewById(R.id.welcomeContinueButton);
        continueButton.setOnClickListener(view -> {
            view.setEnabled(false);
            scheduler.postDelayed(continueSetup, CONTINUE_DELAY_MS);
        });
    }

    @Nullable
    static MainActivityWelcomeScreen showIfNeeded(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState,
            @NonNull Runnable onContinue
    ) {
        if (AppMainUiSettings.isWelcomeCompleted(activity)) {
            return null;
        }
        MainActivityWelcomeScreen screen = new MainActivityWelcomeScreen(activity, onContinue);
        screen.dialog.show();
        screen.dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_WELCOME)) {
            screen.dialog.onRestoreInstanceState(savedInstanceState.getBundle(STATE_WELCOME));
            screen.dialog.findViewById(R.id.welcomeContinueButton).setEnabled(true);
        }
        return screen;
    }

    boolean isShowing() {
        return dialog.isShowing();
    }

    void saveState(@NonNull Bundle outState) {
        if (isShowing()) {
            outState.putBundle(STATE_WELCOME, dialog.onSaveInstanceState());
        }
    }

    void dispose() {
        scheduler.removeCallbacks(continueSetup);
        dialog.dismiss();
    }
}
