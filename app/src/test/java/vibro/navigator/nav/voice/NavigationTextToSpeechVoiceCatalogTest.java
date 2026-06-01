package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class NavigationTextToSpeechVoiceCatalogTest {
    @Test
    public void buildAvailableOptions_filtersUnavailableVoicesAndSortsByLabel() {
        Context context = ApplicationProvider.getApplicationContext();
        Set<Voice> voices = new HashSet<>(Arrays.asList(
                voice("en-us-x-sfg#female_2-local", false, Collections.emptySet()),
                voice("en-us-x-sfg#female_1-local", false, Collections.emptySet()),
                voice("en-us-network", true, Collections.emptySet()),
                voice("en-us-missing", false, Collections.singleton(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        ));

        List<NavigationVoiceOption> options =
                NavigationTextToSpeechVoiceCatalog.buildAvailableOptions(context, voices);

        assertEquals(2, options.size());
        assertEquals("en-us-x-sfg#female_1-local", options.get(0).voiceName);
        assertEquals("en-us-x-sfg#female_2-local", options.get(1).voiceName);
    }

    @Test
    public void findVoice_returnsMatchingVoiceByName() {
        Voice expected = voice("expected", false, Collections.emptySet());
        Set<Voice> voices = new HashSet<>(Arrays.asList(
                voice("other", false, Collections.emptySet()),
                expected
        ));

        assertSame(expected, NavigationTextToSpeechVoiceCatalog.findVoice(voices, "expected"));
    }

    @Test
    public void findVoice_returnsNullForMissingOrNullVoiceSet() {
        assertNull(NavigationTextToSpeechVoiceCatalog.findVoice(
                Collections.singleton(voice("available", false, Collections.emptySet())),
                "missing"
        ));
        assertNull(NavigationTextToSpeechVoiceCatalog.findVoice((Set<Voice>) null, "missing"));
    }

    private static Voice voice(String name, boolean requiresNetwork, Set<String> features) {
        return new Voice(
                name,
                Locale.US,
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                requiresNetwork,
                features
        );
    }
}
