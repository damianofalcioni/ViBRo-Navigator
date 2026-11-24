package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.settings.AppSimulationSettings;

/** Owns the switch and the configuration draft across map picking and Activity recreation. */
final class AboutSimulationSettings {
    private static final int MAP_REQUEST = 4101;
    private final Activity activity;
    private final AboutSimulationDraft draft = new AboutSimulationDraft();
    private AboutDeferredBooleanSetting setting;
    private AboutSimulationDialog dialog;

    AboutSimulationSettings(Activity activity) { this.activity = activity; }

    void restoreState(Bundle state) { draft.restore(activity, state); }

    void configure() {
        Switch switchView = activity.findViewById(R.id.aboutSimulationSwitch);
        setting = new AboutDeferredBooleanSetting(AndroidTaskScheduler.main(),
                enabled -> AppSimulationSettings.setEnabled(activity, enabled), () -> { });
        refresh();
        switchView.setOnCheckedChangeListener((button, enabled) -> setting.set(enabled));
        AboutDeferredDialogAction.configure(activity, activity.findViewById(R.id.aboutSimulationConfigButton),
                () -> { draft.load(activity); showDialog(); });
        AboutDeferredDialogAction.configure(activity, activity.findViewById(R.id.aboutSimulationInfoButton),
                () -> new AlertDialog.Builder(activity).setTitle(R.string.label_simulation)
                        .setMessage(R.string.simulation_info).setPositiveButton(android.R.string.ok, null).show());
        if (draft.open && !draft.picking) { showDialog(); }
    }

    private void showDialog() {
        if (dialog != null && dialog.isShowing()) { return; }
        draft.open = true;
        dialog = new AboutSimulationDialog(activity, draft, this::openMap);
        dialog.show();
    }

    private void openMap(Intent intent) {
        draft.picking = true;
        dialog.dismiss();
        activity.startActivityForResult(intent, MAP_REQUEST);
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != MAP_REQUEST) { return false; }
        draft.picking = false;
        if (resultCode == Activity.RESULT_OK) { draft.applyMapResult(activity, data); }
        showDialog();
        return true;
    }

    void saveState(Bundle state) {
        if (dialog != null) { dialog.capture(); }
        draft.save(state);
    }

    void refresh() {
        if (setting != null) {
            setting.render(activity.findViewById(R.id.aboutSimulationSwitch), AppSimulationSettings.isEnabled(activity));
        }
    }

    void shutdown() {
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }

    void flush() {
        if (setting != null) { setting.flush(false); }
    }
}
