package vibro.navigator.about;

import android.app.Activity;
import android.speech.tts.Voice;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import vibro.navigator.R;
import vibro.navigator.nav.voice.NavigationTextToSpeechVoiceAvailability;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceOptions {
    private AboutManeuverVoiceOptions() {
    }

    @NonNull
    static List<AboutManeuverVoiceSettings.VoiceOption> withBaseOptions(
            @NonNull Activity activity,
            @NonNull List<AboutManeuverVoiceSettings.VoiceOption> availableVoiceOptions
    ) {
        List<AboutManeuverVoiceSettings.VoiceOption> options = new ArrayList<>();
        options.add(new AboutManeuverVoiceSettings.VoiceOption(
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT,
                activity.getString(R.string.label_maneuver_voice_system_default)
        ));
        options.addAll(availableVoiceOptions);
        return options;
    }

    @NonNull
    static List<AboutManeuverVoiceSettings.VoiceOption> buildAvailable(
            @NonNull Activity activity,
            @Nullable Set<Voice> voices
    ) {
        List<AboutManeuverVoiceSettings.VoiceOption> options = new ArrayList<>();
        if (voices == null) {
            return options;
        }
        for (Voice voice : voices) {
            if (NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
                options.add(new AboutManeuverVoiceSettings.VoiceOption(
                        voice.getName(),
                        AboutManeuverVoiceLabelFormatter.format(activity, voice)
                ));
            }
        }
        Collections.sort(options, new Comparator<AboutManeuverVoiceSettings.VoiceOption>() {
            @Override
            public int compare(
                    AboutManeuverVoiceSettings.VoiceOption first,
                    AboutManeuverVoiceSettings.VoiceOption second
            ) {
                return String.CASE_INSENSITIVE_ORDER.compare(first.label, second.label);
            }
        });
        return options;
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

    @Nullable
    static AboutManeuverVoiceSettings.VoiceOption selectedVoiceOption(@Nullable Spinner spinner) {
        if (spinner == null) {
            return null;
        }
        Object selected = spinner.getSelectedItem();
        if (selected instanceof AboutManeuverVoiceSettings.VoiceOption) {
            return (AboutManeuverVoiceSettings.VoiceOption) selected;
        }
        return null;
    }
}
