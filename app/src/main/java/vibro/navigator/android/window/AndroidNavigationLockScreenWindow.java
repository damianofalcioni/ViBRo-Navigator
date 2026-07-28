package vibro.navigator.android.window;

import android.app.Activity;
import android.os.Build;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public final class AndroidNavigationLockScreenWindow {

    private AndroidNavigationLockScreenWindow() {
    }

    public static void allowOverLockScreen(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true);
            activity.setTurnScreenOn(true);
            return;
        }
        allowLegacyOverLockScreen(activity);
    }

    @SuppressWarnings("deprecation")
    private static void allowLegacyOverLockScreen(@NonNull Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
    }
}
