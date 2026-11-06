package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.map.MapPickerActivity;
import vibro.navigator.nav.simulation.NavigationSimulationConfig;
import vibro.navigator.nav.simulation.NavigationSimulationInput;
import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.settings.AppSimulationSettings;

/** Platform dialog keeps Save/Cancel outside the scrollable form. */
final class AboutSimulationDialog {
    interface MapAction { void open(Intent intent); }
    private final Activity activity;
    private final AboutSimulationDraft draft;
    private final AlertDialog dialog;
    private final EditText speed;
    private final EditText position;

    AboutSimulationDialog(Activity activity, AboutSimulationDraft draft, MapAction openMap) {
        this.activity = activity;
        this.draft = draft;
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_simulation, null);
        speed = content.findViewById(R.id.simulationSpeed);
        position = content.findViewById(R.id.simulationPosition);
        TextView label = content.findViewById(R.id.simulationSpeedLabel);
        label.setText(draft.imperial ? R.string.simulation_speed_mph : R.string.simulation_speed_kmh);
        speed.setText(draft.speed);
        position.setText(draft.position);
        AboutDeferredDialogAction.configure(activity, content.findViewById(R.id.simulationMapButton), () -> {
            capture();
            openMap.open(MapPickerActivity.createIntent(activity,
                    activity.getString(R.string.simulation_initial_position),
                    CoordinateParser.tryParse(draft.position, null)));
        });
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.label_simulation).setView(content)
                .setPositiveButton(R.string.simulation_save, null)
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnDismissListener(ignored -> {
            capture();
            draft.open = draft.picking;
        });
    }

    void show() {
        dialog.show();
        AboutDeferredDialogAction.configure(activity, dialog.getButton(AlertDialog.BUTTON_POSITIVE), this::save);
    }

    boolean isShowing() { return dialog.isShowing(); }
    void dismiss() { dialog.dismiss(); }

    void capture() {
        draft.speed = speed.getText().toString();
        draft.position = position.getText().toString().trim();
    }

    private void save() {
        capture();
        Float parsedSpeed = NavigationSimulationInput.parseSpeed(draft.speed, draft.imperial, Locale.getDefault());
        if (parsedSpeed == null) {
            speed.setError(activity.getString(R.string.simulation_invalid_speed));
            return;
        }
        if (!draft.position.isEmpty() && NavigationSimulationConfig.parsePosition(draft.position) == null) {
            position.setError(activity.getString(R.string.simulation_invalid_position));
            return;
        }
        AppSimulationSettings.save(activity, parsedSpeed, draft.position);
        dialog.dismiss();
    }
}
