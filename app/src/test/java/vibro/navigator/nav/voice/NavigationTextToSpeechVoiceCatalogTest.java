package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NavigationTextToSpeechVoiceCatalogTest {
    private static final String FEATURE_NOT_INSTALLED = "notInstalled";

    @Test
    public void buildAvailableOptions_filtersUnavailableVoicesAndSortsByLabel() {
        List<NavigationTextToSpeechVoiceDescriptor> voices = Arrays.asList(
                voice("en-us-x-sfg#female_2-local", false, Collections.emptySet()),
                voice("en-us-x-sfg#female_1-local", false, Collections.emptySet()),
                voice("en-us-network", true, Collections.emptySet()),
                voice("en-us-missing", false, Collections.singleton(FEATURE_NOT_INSTALLED))
        );

        List<NavigationVoiceOption> options =
                NavigationTextToSpeechVoiceCatalog.buildAvailableOptions(
                        NavigationTextToSpeechVoiceDescriptor::name,
                        voices
                );

        assertEquals(2, options.size());
        assertEquals("en-us-x-sfg#female_1-local", options.get(0).voiceName);
        assertEquals("en-us-x-sfg#female_2-local", options.get(1).voiceName);
    }

    @Test
    public void findVoice_returnsMatchingVoiceByName() {
        NavigationTextToSpeechVoiceDescriptor expected = voice("expected", false, Collections.emptySet());
        List<NavigationTextToSpeechVoiceDescriptor> voices = Arrays.asList(
                voice("other", false, Collections.emptySet()),
                expected
        );

        assertSame(expected, NavigationTextToSpeechVoiceCatalog.findVoiceDescriptor(voices, "expected"));
    }

    @Test
    public void findVoice_returnsNullForMissingOrNullVoiceSet() {
        assertNull(NavigationTextToSpeechVoiceCatalog.findVoiceDescriptor(
                Collections.singleton(voice("available", false, Collections.emptySet())),
                "missing"
        ));
        assertNull(NavigationTextToSpeechVoiceCatalog.findVoiceDescriptor(null, "missing"));
    }

    private static NavigationTextToSpeechVoiceDescriptor voice(
            String name,
            boolean requiresNetwork,
            Set<String> features
    ) {
        return new NavigationTextToSpeechVoiceDescriptor(
                name,
                Locale.US,
                requiresNetwork,
                features
        );
    }
}
