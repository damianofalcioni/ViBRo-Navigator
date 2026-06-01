package vibro.navigator.nav.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class NavigationTextToSpeechVoiceCatalog {
    private NavigationTextToSpeechVoiceCatalog() {
    }

    @NonNull
    static List<NavigationVoiceOption> buildAvailableOptions(
            @NonNull Context context,
            @Nullable Set<Voice> voices
    ) {
        List<NavigationVoiceOption> options = new ArrayList<>();
        if (voices == null) {
            return options;
        }
        for (Voice voice : voices) {
            if (NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
                options.add(new NavigationVoiceOption(
                        voice.getName(),
                        NavigationManeuverVoiceLabelFormatter.format(context, voice)
                ));
            }
        }
        Collections.sort(options, new Comparator<NavigationVoiceOption>() {
            @Override
            public int compare(NavigationVoiceOption first, NavigationVoiceOption second) {
                return String.CASE_INSENSITIVE_ORDER.compare(first.label, second.label);
            }
        });
        return options;
    }

    @Nullable
    static Voice findVoice(@NonNull TextToSpeech tts, @NonNull String voiceName) {
        return findVoice(tts.getVoices(), voiceName);
    }

    @Nullable
    static Voice findVoice(@Nullable Set<Voice> voices, @NonNull String voiceName) {
        if (voices == null) {
            return null;
        }
        for (Voice voice : voices) {
            if (voiceName.equals(voice.getName())) {
                return voice;
            }
        }
        return null;
    }
}
