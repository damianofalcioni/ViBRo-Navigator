package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.voice.NavigationTextToSpeechSettingsClient;
import vibro.navigator.nav.voice.NavigationVoiceOption;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceSettings {
    private static final String TAG = "AboutManeuverVoice";

    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch enabledSwitch;
    @Nullable
    private NavigationTextToSpeechSettingsClient voiceClient;
    @Nullable
    private AboutManeuverVoiceOptionAdapter voiceAdapter;
    @Nullable
    private Spinner dialogSpinner;
    private boolean renderingVoiceSelection;
    private boolean renderingEnabledSwitch;
    private boolean voiceListLoaded;

    AboutManeuverVoiceSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch
    ) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
        renderOptions(Collections.emptyList());
        configureEnabledSwitch();
        AboutDeferredDialogAction.configure(activity, settingsButton, this::showDialog);
        initializeVoiceClient();
    }

    void refreshSelection() {
        refreshEnabledSwitch();
        selectSavedVoiceOrUseFallback();
    }

    void shutdown() {
        if (voiceClient == null) {
            return;
        }
        voiceClient.shutdown();
        voiceClient = null;
    }

    private void initializeVoiceClient() {
        try {
            NavigationTextToSpeechSettingsClient client = new NavigationTextToSpeechSettingsClient(
                    activity.getApplicationContext(),
                    this::onAvailableVoicesLoaded
            );
            voiceClient = client;
            client.initialize();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to initialize maneuver voice settings client", e);
        }
    }

    private void onAvailableVoicesLoaded(@NonNull List<NavigationVoiceOption> options) {
        voiceListLoaded = true;
        activity.runOnUiThread(() -> renderOptions(options));
    }

    private void renderOptions(@NonNull List<NavigationVoiceOption> availableVoiceOptions) {
        List<NavigationVoiceOption> options = AboutManeuverVoiceOptions.withBaseOptions(activity, availableVoiceOptions);

        voiceAdapter = new AboutManeuverVoiceOptionAdapter(activity, options);

        if (dialogSpinner != null) {
            configureSpinner(dialogSpinner);
            return;
        }
        selectSavedVoiceOrUseFallback();
    }

    private void configureEnabledSwitch() {
        refreshEnabledSwitch();
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (renderingEnabledSwitch) {
                return;
            }
            AppSettings.setManeuverSpeechEnabled(activity, isChecked);
        });
    }

    private void refreshEnabledSwitch() {
        renderingEnabledSwitch = true;
        enabledSwitch.setChecked(AppSettings.isManeuverSpeechEnabled(activity));
        renderingEnabledSwitch = false;
    }

    private void showDialog() {
        AboutManeuverVoiceDialog.show(activity, new AboutManeuverVoiceDialog.VoiceSpinnerHandler() {
            @Override
            public void configure(@NonNull Spinner spinner) {
                dialogSpinner = spinner;
                configureSpinner(spinner);
            }

            @Override
            public void playSelectedVoice() {
                speakSelectedVoicePreview();
            }

            @Override
            public void clear() {
                dialogSpinner = null;
            }
        });
    }

    private void speakSelectedVoicePreview() {
        NavigationVoiceOption selected = AboutManeuverVoiceOptions.selectedVoiceOption(dialogSpinner);
        if (selected != null && voiceClient != null) {
            voiceClient.speakPreview(selected.voiceName);
        }
    }

    private void configureSpinner(@NonNull Spinner spinner) {
        renderingVoiceSelection = true;
        spinner.setOnItemSelectedListener(null);
        spinner.setAdapter(voiceAdapter);
        selectSavedVoiceOrUseFallback();
        renderingVoiceSelection = false;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (renderingVoiceSelection) {
                    return;
                }
                Object selected = parent.getItemAtPosition(position);
                if (selected instanceof NavigationVoiceOption) {
                    persistSelectedVoiceIfNeeded((NavigationVoiceOption) selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void persistSelectedVoiceIfNeeded(@NonNull NavigationVoiceOption selected) {
        String savedVoiceName = AppSettings.getManeuverVoiceName(activity);
        if (shouldPersistSelectedVoice(voiceListLoaded, savedVoiceName, selected.voiceName)) {
            AppSettings.setManeuverVoiceName(activity, selected.voiceName);
            updateSelectedVoiceHighlight(selected.voiceName);
        }
    }

    private void selectSavedVoiceOrUseFallback() {
        String fallbackVoiceName = AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT;
        if (selectVoice(AppSettings.getManeuverVoiceName(activity))) {
            return;
        }
        if (voiceListLoaded && AppSettings.isManeuverSpeechEnabled(activity)) {
            AppSettings.setManeuverVoiceName(activity, fallbackVoiceName);
        }
        selectVoice(fallbackVoiceName);
    }

    private boolean selectVoice(@NonNull String voiceName) {
        int position = findVoicePosition(voiceName);
        if (position < 0) {
            return false;
        }
        updateSelectedVoiceHighlight(voiceName);
        if (dialogSpinner != null && dialogSpinner.getCount() > position) {
            dialogSpinner.setSelection(position);
        }
        return true;
    }

    private int findVoicePosition(@NonNull String voiceName) {
        if (voiceAdapter == null) {
            return -1;
        }
        for (int i = 0; i < voiceAdapter.getCount(); i++) {
            NavigationVoiceOption option = voiceAdapter.getItem(i);
            if (option != null && option.voiceName.equals(voiceName)) {
                return i;
            }
        }
        return -1;
    }

    static boolean shouldPersistSelectedVoice(
            boolean voiceListLoaded,
            @NonNull String savedVoiceName,
            @NonNull String selectedVoiceName
    ) {
        return AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                voiceListLoaded,
                savedVoiceName,
                selectedVoiceName
        );
    }

    private void updateSelectedVoiceHighlight(@NonNull String voiceName) {
        if (voiceAdapter != null) {
            voiceAdapter.setSelectedVoiceName(voiceName);
        }
    }
}
