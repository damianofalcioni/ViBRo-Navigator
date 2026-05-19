package vibro.navigator.nav.voice;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationSpeechAudioFocus {
    private static final int FOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
    private static final int LEGACY_STREAM_TYPE = AudioManager.STREAM_MUSIC;

    @Nullable
    private final AudioManager audioManager;
    @NonNull
    private final AudioAttributes audioAttributes;
    @NonNull
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener;
    @Nullable
    private Object audioFocusRequest;
    private boolean focusHeld;

    NavigationSpeechAudioFocus(@NonNull Context context) {
        audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioAttributes = createAudioAttributes();
        focusChangeListener = ignored -> {
        };
    }

    @NonNull
    AudioAttributes audioAttributes() {
        return audioAttributes;
    }

    synchronized boolean requestTransientMayDuckFocus() {
        if (audioManager == null) {
            return true;
        }
        int result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? requestTransientMayDuckFocusApi26()
                : audioManager.requestAudioFocus(focusChangeListener, LEGACY_STREAM_TYPE, FOCUS_GAIN);
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return focusHeld;
    }

    synchronized void abandonFocus() {
        if (!focusHeld || audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            Api26.abandonAudioFocus(audioManager, audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        focusHeld = false;
    }

    @NonNull
    static AudioAttributes createAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private int requestTransientMayDuckFocusApi26() {
        if (audioFocusRequest == null) {
            audioFocusRequest = Api26.createAudioFocusRequest(audioAttributes);
        }
        return Api26.requestAudioFocus(audioManager, audioFocusRequest);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static final class Api26 {
        private Api26() {
        }

        @NonNull
        static AudioFocusRequest createAudioFocusRequest(@NonNull AudioAttributes attributes) {
            return new AudioFocusRequest.Builder(FOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .build();
        }

        static int requestAudioFocus(
                @NonNull AudioManager audioManager,
                @NonNull Object audioFocusRequest
        ) {
            return audioManager.requestAudioFocus((AudioFocusRequest) audioFocusRequest);
        }

        static void abandonAudioFocus(
                @NonNull AudioManager audioManager,
                @NonNull Object audioFocusRequest
        ) {
            audioManager.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequest);
        }
    }
}
