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
    interface VoiceLabelFormatter {
        @NonNull
        String format(@NonNull NavigationTextToSpeechVoiceDescriptor voice);
    }

    private NavigationTextToSpeechVoiceCatalog() {
    }

    @NonNull
    static List<NavigationVoiceOption> buildAvailableOptions(
            @NonNull Context context,
            @Nullable Set<Voice> voices
    ) {
        return buildAvailableOptions(
                voice -> NavigationManeuverVoiceLabelFormatter.format(context, voice),
                descriptors(voices)
        );
    }

    @NonNull
    static List<NavigationVoiceOption> buildAvailableOptions(
            @NonNull VoiceLabelFormatter labelFormatter,
            @Nullable Iterable<NavigationTextToSpeechVoiceDescriptor> voices
    ) {
        List<NavigationVoiceOption> options = new ArrayList<>();
        if (voices == null) {
            return options;
        }
        for (NavigationTextToSpeechVoiceDescriptor voice : voices) {
            if (NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
                options.add(new NavigationVoiceOption(
                        voice.name(),
                        labelFormatter.format(voice)
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

    @NonNull
    private static List<NavigationTextToSpeechVoiceDescriptor> descriptors(@Nullable Set<Voice> voices) {
        List<NavigationTextToSpeechVoiceDescriptor> descriptors = new ArrayList<>();
        if (voices == null) {
            return descriptors;
        }
        for (Voice voice : voices) {
            descriptors.add(NavigationTextToSpeechVoiceDescriptor.from(voice));
        }
        return descriptors;
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

    @Nullable
    static NavigationTextToSpeechVoiceDescriptor findVoiceDescriptor(
            @Nullable Iterable<NavigationTextToSpeechVoiceDescriptor> voices,
            @NonNull String voiceName
    ) {
        if (voices == null) {
            return null;
        }
        for (NavigationTextToSpeechVoiceDescriptor voice : voices) {
            if (voiceName.equals(voice.name())) {
                return voice;
            }
        }
        return null;
    }
}
