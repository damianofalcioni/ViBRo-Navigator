package vibro.navigator.about;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.voice.NavigationTextToSpeechVoiceAvailability;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceSettings {
    private static final String TAG = "AboutManeuverVoice";

    @NonNull
    private final Activity activity;
    @NonNull
    private final Spinner spinner;
    @Nullable
    private TextToSpeech tts;
    private boolean rendering;
    private boolean initCallbackReceived;
    private boolean voiceListLoaded;
    private int initStatus = TextToSpeech.ERROR;

    AboutManeuverVoiceSettings(@NonNull Activity activity, @NonNull Spinner spinner) {
        this.activity = activity;
        this.spinner = spinner;
        renderOptions(Collections.emptyList());
        initializeTextToSpeech();
    }

    void refreshSelection() {
        selectSavedVoiceOrDisableUnavailable();
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
        activity.runOnUiThread(() -> renderOptions(buildVoiceOptions(voices)));
    }

    private void renderOptions(@NonNull List<VoiceOption> availableVoiceOptions) {
        List<VoiceOption> options = baseOptions();
        options.addAll(availableVoiceOptions);

        ArrayAdapter<VoiceOption> adapter = new ArrayAdapter<>(
                activity,
                R.layout.item_profile_spinner,
                options
        );
        adapter.setDropDownViewResource(R.layout.item_profile_spinner_dropdown);

        rendering = true;
        spinner.setAdapter(adapter);
        selectSavedVoiceOrDisableUnavailable();
        rendering = false;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (rendering) {
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
        }
    }

    @NonNull
    private List<VoiceOption> baseOptions() {
        List<VoiceOption> options = new ArrayList<>();
        options.add(new VoiceOption(
                AppSettings.MANEUVER_VOICE_DISABLED,
                activity.getString(R.string.label_maneuver_voice_disabled)
        ));
        options.add(new VoiceOption(
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT,
                activity.getString(R.string.label_maneuver_voice_system_default)
        ));
        return options;
    }

    private void selectSavedVoiceOrDisableUnavailable() {
        if (selectSavedVoice()) {
            return;
        }
        if (voiceListLoaded) {
            AppSettings.setManeuverVoiceName(activity, AppSettings.MANEUVER_VOICE_DISABLED);
        }
        if (spinner.getCount() > 0) {
            spinner.setSelection(0);
        }
    }

    private boolean selectSavedVoice() {
        String savedVoiceName = AppSettings.getManeuverVoiceName(activity);
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item instanceof VoiceOption && ((VoiceOption) item).voiceName.equals(savedVoiceName)) {
                spinner.setSelection(i);
                return true;
            }
        }
        return false;
    }

    static boolean shouldPersistSelectedVoice(
            boolean voiceListLoaded,
            @NonNull String savedVoiceName,
            @NonNull String selectedVoiceName
    ) {
        if (selectedVoiceName.equals(savedVoiceName)) {
            return false;
        }
        return voiceListLoaded
                || isBaseVoiceName(savedVoiceName)
                || !isBaseVoiceName(selectedVoiceName);
    }

    private static boolean isBaseVoiceName(@NonNull String voiceName) {
        return AppSettings.MANEUVER_VOICE_DISABLED.equals(voiceName)
                || AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT.equals(voiceName);
    }

    @NonNull
    private List<VoiceOption> buildVoiceOptions(@Nullable Set<Voice> voices) {
        List<VoiceOption> options = new ArrayList<>();
        if (voices == null) {
            return options;
        }
        for (Voice voice : voices) {
            if (NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
                options.add(new VoiceOption(voice.getName(), formatVoiceLabel(voice)));
            }
        }
        Collections.sort(options, new Comparator<VoiceOption>() {
            @Override
            public int compare(VoiceOption first, VoiceOption second) {
                return String.CASE_INSENSITIVE_ORDER.compare(first.label, second.label);
            }
        });
        return options;
    }

    @NonNull
    private String formatVoiceLabel(@NonNull Voice voice) {
        Locale locale = voice.getLocale();
        if (locale == null) {
            return voice.getName();
        }
        return activity.getString(
                R.string.format_maneuver_voice_option,
                locale.getDisplayName(),
                voice.getName()
        );
    }

    private static final class VoiceOption {
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
