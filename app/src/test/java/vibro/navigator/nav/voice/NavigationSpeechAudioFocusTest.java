package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAudioManager;

@RunWith(RobolectricTestRunner.class)
public class NavigationSpeechAudioFocusTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void audioAttributesUseNavigationGuidanceSpeech() {
        AudioAttributes attributes = NavigationSpeechAudioFocus.createAudioAttributes();

        assertEquals(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, attributes.getUsage());
        assertEquals(AudioAttributes.CONTENT_TYPE_SPEECH, attributes.getContentType());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void requestTransientMayDuckFocus_onOreoAndLaterUsesAudioFocusRequestAttributes() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        ShadowAudioManager shadowAudioManager = shadowOf(audioManager);
        NavigationSpeechAudioFocus focus = new NavigationSpeechAudioFocus(context);

        assertTrue(focus.requestTransientMayDuckFocus());

        ShadowAudioManager.AudioFocusRequest request = shadowAudioManager.getLastAudioFocusRequest();
        assertNotNull(request.audioFocusRequest);
        assertEquals(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                request.audioFocusRequest.getFocusGain()
        );
        assertEquals(
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
                request.audioFocusRequest.getAudioAttributes().getUsage()
        );
        assertEquals(
                AudioAttributes.CONTENT_TYPE_SPEECH,
                request.audioFocusRequest.getAudioAttributes().getContentType()
        );

        focus.abandonFocus();

        assertSame(request.audioFocusRequest, shadowAudioManager.getLastAbandonedAudioFocusRequest());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void requestTransientMayDuckFocus_beforeOreoUsesLegacyMusicStreamFocus() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        ShadowAudioManager shadowAudioManager = shadowOf(audioManager);
        NavigationSpeechAudioFocus focus = new NavigationSpeechAudioFocus(context);

        assertTrue(focus.requestTransientMayDuckFocus());

        ShadowAudioManager.AudioFocusRequest request = shadowAudioManager.getLastAudioFocusRequest();
        assertNull(request.audioFocusRequest);
        assertEquals(AudioManager.STREAM_MUSIC, request.streamType);
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, request.durationHint);

        focus.abandonFocus();

        assertSame(request.listener, shadowAudioManager.getLastAbandonedAudioFocusListener());
    }
}
