package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetResources;

final class AboutNavigationCustomButtonSettings {
    @NonNull
    private final Activity activity;
    @NonNull
    private final View settingsButton;
    @NonNull
    private final Target[] targets = AppNavigationCustomButtonTargetResources.selectableTargets();
    private boolean renderingSelection;

    AboutNavigationCustomButtonSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton
    ) {
        this.activity = activity;
        this.settingsButton = settingsButton;
    }

    void configure() {
        AboutDeferredDialogAction.configure(activity, settingsButton, this::showDialog);
    }

    private void showDialog() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView label = createLabel();
        Spinner spinner = createSpinner();
        content.addView(label);
        content.addView(spinner);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.label_navigation_custom_button)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @NonNull
    private TextView createLabel() {
        TextView label = new TextView(activity);
        label.setId(R.id.aboutNavigationCustomButtonTargetLabel);
        label.setText(R.string.label_navigation_custom_button_target);
        label.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return label;
    }

    @NonNull
    private Spinner createSpinner() {
        Spinner spinner = new Spinner(activity);
        spinner.setId(R.id.aboutNavigationCustomButtonTargetSpinner);
        spinner.setAdapter(newAdapter());
        selectCurrentTarget(spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!renderingSelection && position >= 0 && position < targets.length) {
                    AppNavigationCustomButtonSettings.setTarget(activity, targets[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return spinner;
    }

    @NonNull
    private ArrayAdapter<String> newAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_item,
                targetLabels()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    @NonNull
    private List<String> targetLabels() {
        List<String> labels = new ArrayList<>();
        for (Target target : targets) {
            labels.add(activity.getString(AppNavigationCustomButtonTargetResources.labelResId(target)));
        }
        return labels;
    }

    private void selectCurrentTarget(@NonNull Spinner spinner) {
        renderingSelection = true;
        try {
            spinner.setSelection(positionOf(AppNavigationCustomButtonSettings.getTarget(activity)));
        } finally {
            renderingSelection = false;
        }
    }

    private int positionOf(@NonNull Target target) {
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == target) {
                return i;
            }
        }
        return 0;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}
