package vibro.navigator.about;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceSettings {
    private static final String TAG = "AboutManeuverVoice";

    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch enabledSwitch;
    @Nullable
    private TextToSpeech tts;
    @Nullable
    private AboutManeuverVoiceOptionAdapter voiceAdapter;
    @Nullable
    private Spinner dialogSpinner;
    private boolean renderingVoiceSelection;
    private boolean renderingEnabledSwitch;
    private boolean initCallbackReceived;
    private boolean voiceListLoaded;
    private int initStatus = TextToSpeech.ERROR;

    AboutManeuverVoiceSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch
    ) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
        renderOptions(Collections.emptyList());
        configureEnabledSwitch();
        settingsButton.setOnClickListener(v -> showDialog());
        initializeTextToSpeech();
    }

    void refreshSelection() {
        refreshEnabledSwitch();
        selectSavedVoiceOrUseFallback();
    }

    void shutdown() {
        if (tts == null) {
            return;
        }
        tts.shutdown();
        tts = null;
    }

    private void initializeTextToSpeech() {
        try {
            TextToSpeech engine = new TextToSpeech(activity.getApplicationContext(), this::onTextToSpeechInit);
            tts = engine;
            handleTextToSpeechInitIfReady();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to initialize TextToSpeech for voice settings", e);
        }
    }

    private void onTextToSpeechInit(int status) {
        initStatus = status;
        initCallbackReceived = true;
        handleTextToSpeechInitIfReady();
    }

    private void handleTextToSpeechInitIfReady() {
        if (!initCallbackReceived || tts == null) {
            return;
        }
        if (initStatus != TextToSpeech.SUCCESS) {
            AppLogger.w(TAG, "TextToSpeech voice listing unavailable status=" + initStatus);
            return;
        }
        Set<Voice> voices = tts.getVoices();
        voiceListLoaded = true;
        activity.runOnUiThread(() -> renderOptions(AboutManeuverVoiceOptions.buildAvailable(activity, voices)));
    }

    private void renderOptions(@NonNull List<VoiceOption> availableVoiceOptions) {
        List<VoiceOption> options = AboutManeuverVoiceOptions.withBaseOptions(activity, availableVoiceOptions);

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
        VoiceOption selected = AboutManeuverVoiceOptions.selectedVoiceOption(dialogSpinner);
        if (selected != null) {
            AboutManeuverVoicePreview.speak(activity, tts, selected.voiceName);
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
                if (selected instanceof VoiceOption) {
                    persistSelectedVoiceIfNeeded((VoiceOption) selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void persistSelectedVoiceIfNeeded(@NonNull VoiceOption selected) {
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
            VoiceOption option = voiceAdapter.getItem(i);
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

    static final class VoiceOption {
        @NonNull
        final String voiceName;
        @NonNull
        final String label;

        VoiceOption(@NonNull String voiceName, @NonNull String label) {
            this.voiceName = voiceName;
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}
