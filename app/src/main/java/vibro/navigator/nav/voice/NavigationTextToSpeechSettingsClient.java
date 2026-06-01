package vibro.navigator.nav.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

public final class NavigationTextToSpeechSettingsClient {
    public interface Callback {
        void onAvailableVoicesLoaded(@NonNull List<NavigationVoiceOption> availableVoiceOptions);
    }

    private static final String TAG = "ManeuverVoiceSettings";

    @NonNull
    private final Context appContext;
    @NonNull
    private final Callback callback;
    @Nullable
    private TextToSpeech tts;
    private boolean initCallbackReceived;
    private int initStatus = TextToSpeech.ERROR;

    public NavigationTextToSpeechSettingsClient(@NonNull Context context, @NonNull Callback callback) {
        appContext = context.getApplicationContext();
        this.callback = callback;
    }

    public void initialize() {
        try {
            TextToSpeech engine = new TextToSpeech(appContext, this::onTextToSpeechInit);
            tts = engine;
            handleTextToSpeechInitIfReady();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to initialize TextToSpeech for voice settings", e);
        }
    }

    public void speakPreview(@NonNull String voiceName) {
        if (tts == null) {
            AppLogger.w(TAG, "TextToSpeech preview requested before engine initialization");
            return;
        }
        if (!applyVoice(tts, voiceName)) {
            return;
        }
        int result = tts.speak(
                sampleText(),
                TextToSpeech.QUEUE_FLUSH,
                new Bundle(),
                "maneuver-preview"
        );
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech failed to speak preview");
        }
    }

    public void shutdown() {
        if (tts == null) {
            return;
        }
        tts.shutdown();
        tts = null;
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
        callback.onAvailableVoicesLoaded(buildAvailable(tts.getVoices()));
    }

    @NonNull
    private List<NavigationVoiceOption> buildAvailable(@Nullable Set<Voice> voices) {
        List<NavigationVoiceOption> options = new ArrayList<>();
        if (voices == null) {
            return options;
        }
        for (Voice voice : voices) {
            if (NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
                options.add(new NavigationVoiceOption(
                        voice.getName(),
                        NavigationManeuverVoiceLabelFormatter.format(appContext, voice)
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

    private boolean applyVoice(@NonNull TextToSpeech engine, @NonNull String voiceName) {
        if (AppSettings.isSystemDefaultManeuverVoice(voiceName)) {
            return applySystemDefaultVoice(engine);
        }
        Voice voice = findVoice(engine, voiceName);
        if (voice == null) {
            AppLogger.w(TAG, "Configured TextToSpeech preview voice unavailable: " + voiceName);
            return false;
        }
        int result = engine.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected preview voice: " + voiceName);
            return false;
        }
        return true;
    }

    private boolean applySystemDefaultVoice(@NonNull TextToSpeech engine) {
        Voice voice = engine.getDefaultVoice();
        if (voice == null) {
            return true;
        }
        int result = engine.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected default preview voice");
            return false;
        }
        return true;
    }

    @Nullable
    private static Voice findVoice(@NonNull TextToSpeech engine, @NonNull String voiceName) {
        Set<Voice> voices = engine.getVoices();
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

    @NonNull
    private String sampleText() {
        return appContext.getString(
                R.string.format_turn_speech,
                appContext.getResources().getQuantityString(R.plurals.format_time_speech_seconds, 20, 20),
                appContext.getString(R.string.direction_turn_left)
        );
    }
}
