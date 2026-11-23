package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.service.NavigationServiceBinder;

/** Shows the service's one-time notice when the phone screen is in the foreground. */
final class NavigationSimulationNotice {
    private final Activity activity;
    private boolean resumed;
    @Nullable private AlertDialog dialog;

    NavigationSimulationNotice(Activity activity) {
        this.activity = activity;
    }

    void onResume(@Nullable NavigationServiceBinder binder) {
        resumed = true;
        showIfPending(binder);
    }

    void onPause() {
        resumed = false;
        dismiss();
    }

    void showIfPending(@Nullable NavigationServiceBinder binder) {
        if (!resumed || activity.isFinishing() || binder == null || !binder.consumeSimulationStartNotice()) {
            return;
        }
        dismiss();
        dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_simulation)
                .setMessage(R.string.nav_simulation_started)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
