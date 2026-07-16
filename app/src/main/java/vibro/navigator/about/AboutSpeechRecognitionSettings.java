package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.speech.AndroidSpeechRecognitionSupport;
import vibro.navigator.settings.AppSpeechRecognitionSettings;

final class AboutSpeechRecognitionSettings {
    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch enabledSwitch;
    @NonNull
    private final ProviderAvailability providerAvailability;
    @NonNull
    private final ArrayAdapter<AboutSpeechRecognitionLanguageOption> languageAdapter;
    @Nullable
    private Spinner dialogSpinner;
    private boolean renderingLanguageSelection;
    private boolean renderingEnabledSwitch;

    AboutSpeechRecognitionSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch
    ) {
        this(
                activity,
                settingsButton,
                enabledSwitch,
                () -> AndroidSpeechRecognitionSupport.isAvailable(activity)
        );
    }

    AboutSpeechRecognitionSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch,
            @NonNull ProviderAvailability providerAvailability
    ) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
        this.providerAvailability = providerAvailability;
        List<AboutSpeechRecognitionLanguageOption> options =
                AboutSpeechRecognitionLanguageOptions.defaultOptions(activity);
        languageAdapter = new ArrayAdapter<>(activity, R.layout.item_profile_spinner, options);
        languageAdapter.setDropDownViewResource(R.layout.item_profile_spinner_dropdown);
        configureEnabledSwitch();
        AboutDeferredDialogAction.configure(activity, settingsButton, this::showDialog);
    }

    void refreshSelection() {
        refreshEnabledSwitch();
        selectSavedLanguage();
    }

    private void showDialog() {
        AboutSpeechRecognitionDialog.show(activity, new AboutSpeechRecognitionDialog.LanguageSpinnerHandler() {
            @Override
            public void configure(@NonNull Spinner spinner) {
                dialogSpinner = spinner;
                configureSpinner(spinner);
            }

            @Override
            public void clear() {
                dialogSpinner = null;
            }
        });
    }

    private void configureSpinner(@NonNull Spinner spinner) {
        renderingLanguageSelection = true;
        spinner.setOnItemSelectedListener(null);
        spinner.setAdapter(languageAdapter);
        selectSavedLanguage();
        renderingLanguageSelection = false;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (renderingLanguageSelection) {
                    return;
                }
                Object selected = parent.getItemAtPosition(position);
                if (selected instanceof AboutSpeechRecognitionLanguageOption) {
                    persistSelectedLanguage((AboutSpeechRecognitionLanguageOption) selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void persistSelectedLanguage(@NonNull AboutSpeechRecognitionLanguageOption selected) {
        AppSpeechRecognitionSettings.setLanguageTag(activity, selected.languageTag);
    }

    private void configureEnabledSwitch() {
        refreshEnabledSwitch();
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (renderingEnabledSwitch) {
                return;
            }
            AppSpeechRecognitionSettings.setEnabled(activity, isChecked);
        });
    }

    private void refreshEnabledSwitch() {
        boolean available = providerAvailability.isAvailable();
        renderingEnabledSwitch = true;
        enabledSwitch.setEnabled(available);
        enabledSwitch.setChecked(available && AppSpeechRecognitionSettings.isEnabled(activity));
        renderingEnabledSwitch = false;
    }

    private void selectSavedLanguage() {
        if (dialogSpinner == null) {
            return;
        }
        int position = findLanguagePosition(AppSpeechRecognitionSettings.getLanguageTag(activity));
        dialogSpinner.setSelection(Math.max(position, 0));
    }

    private int findLanguagePosition(@NonNull String languageTag) {
        for (int i = 0; i < languageAdapter.getCount(); i++) {
            AboutSpeechRecognitionLanguageOption option = languageAdapter.getItem(i);
            if (option != null && option.languageTag.equals(languageTag)) {
                return i;
            }
        }
        return -1;
    }

    interface ProviderAvailability {
        boolean isAvailable();
    }
}
