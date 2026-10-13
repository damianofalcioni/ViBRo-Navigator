package vibro.navigator.about;

import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.android.language.AndroidLocalizedActivity;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppLanguageSettings;

final class AboutLanguageSettings {
    private final AndroidLocalizedActivity activity;

    AboutLanguageSettings(AndroidLocalizedActivity activity) {
        this.activity = activity;
    }

    void configure() {
        Switch systemSwitch = activity.findViewById(R.id.aboutUseSystemLanguageSwitch);
        systemSwitch.setOnCheckedChangeListener(null);
        systemSwitch.setChecked(AppLanguageSettings.isUseSystemLanguageEnabled(activity));
        systemSwitch.setOnCheckedChangeListener((button, checked) -> {
            AppLanguageSettings.setUseSystemLanguageEnabled(activity, checked, AndroidAppLanguage.systemLanguageTag());
            activity.recreateIfLanguageChanged();
        });
        AboutDeferredDialogAction.configure(activity,
                activity.findViewById(R.id.aboutLanguageSettingsButton), this::showDialog);
    }

    private void showDialog() {
        String[] languageTags = AppLanguageSettings.availableLanguageTags();
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding / 2);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(activity);
        label.setText(R.string.label_app_language);
        label.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        row.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Spinner spinner = new Spinner(activity);
        spinner.setId(R.id.aboutLanguageSpinner);
        spinner.setContentDescription(activity.getString(R.string.label_app_language));
        spinner.setBackgroundResource(R.drawable.bg_spinner_field);
        spinner.setPopupBackgroundResource(R.drawable.bg_spinner_popup);
        spinner.setMinimumHeight(Math.round(48 * activity.getResources().getDisplayMetrics().density));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                R.layout.item_profile_spinner, languageNames(languageTags));
        adapter.setDropDownViewResource(R.layout.item_profile_spinner_dropdown);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedPosition(languageTags));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        spinnerParams.setMarginStart(Math.round(12 * activity.getResources().getDisplayMetrics().density));
        row.addView(spinner, spinnerParams);
        content.addView(row);
        TextView explanation = new TextView(activity);
        explanation.setText(R.string.hint_app_language);
        explanation.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextSecondaryColor));
        content.addView(explanation);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_app_language)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    AppLanguageSettings.setSelectedLanguageTag(activity, languageTags[spinner.getSelectedItemPosition()]);
                    activity.recreateIfLanguageChanged();
                })
                .show();
    }

    private int selectedPosition(String[] languageTags) {
        String selected = AppLanguageSettings.getSelectedLanguageTag(activity, AndroidAppLanguage.systemLanguageTag());
        for (int index = 0; index < languageTags.length; index++) {
            if (selected.equals(languageTags[index])) {
                return index;
            }
        }
        return 0;
    }

    private List<String> languageNames(String[] languageTags) {
        List<String> names = new ArrayList<>();
        for (String tag : languageTags) {
            Locale locale = Locale.forLanguageTag(tag);
            String name = locale.getDisplayLanguage(locale);
            names.add(name.substring(0, 1).toUpperCase(locale) + name.substring(1));
        }
        return names;
    }
}
