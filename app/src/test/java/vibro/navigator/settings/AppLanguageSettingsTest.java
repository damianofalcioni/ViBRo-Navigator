package vibro.navigator.settings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppLanguageSettingsTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void defaultsFollowSystemAndPickerMatchesSystem() {
        assertTrue(AppLanguageSettings.isUseSystemLanguageEnabled(context));
        assertEquals("it", AppLanguageSettings.getEffectiveLanguageTag(context, "it-IT"));
        assertEquals("de", AppLanguageSettings.getSelectedLanguageTag(context, "de-AT"));
    }

    @Test
    public void manualModeFreezesInitialSystemChoice() {
        AppLanguageSettings.setUseSystemLanguageEnabled(context, false, "de-AT");
        assertEquals("de", AppLanguageSettings.getEffectiveLanguageTag(context, "it-IT"));
    }

    @Test
    public void selectionIsRememberedWhileAutomaticModeIgnoresIt() {
        AppLanguageSettings.setSelectedLanguageTag(context, "it");
        assertEquals("de", AppLanguageSettings.getEffectiveLanguageTag(context, "de"));
        AppLanguageSettings.setUseSystemLanguageEnabled(context, false, "de");
        assertEquals("it", AppLanguageSettings.getEffectiveLanguageTag(context, "de"));
        AppLanguageSettings.setUseSystemLanguageEnabled(context, true, "de");
        assertEquals("de", AppLanguageSettings.getEffectiveLanguageTag(context, "de"));
        assertEquals("it", AppLanguageSettings.getSelectedLanguageTag(context, "de"));
    }

    @Test
    public void unsupportedLanguagesFallBackToEnglishAndCatalogCannotBeMutated() {
        assertEquals("en", AppLanguageSettings.getEffectiveLanguageTag(context, "fi-FI"));
        AppLanguageSettings.setSelectedLanguageTag(context, "invalid");
        assertEquals("en", AppLanguageSettings.getSelectedLanguageTag(context, "it"));
        AppLanguageSettings.availableLanguageTags()[0] = "fr";
        assertArrayEquals(new String[]{"en", "it", "de", "ar", "ca", "cs", "da", "el", "es", "fr",
                "hu", "ja", "ko", "nl", "pl", "pt", "sv", "zh"}, AppLanguageSettings.availableLanguageTags());
    }

    @Test
    public void addedLanguagesMatchRegionalVariantsAndPersistManualSelection() {
        for (String tag : AppLanguageSettings.availableLanguageTags()) {
            assertEquals(tag, AppLanguageSettings.getEffectiveLanguageTag(context, tag + "-XX"));
            AppLanguageSettings.setSelectedLanguageTag(context, tag);
            AppLanguageSettings.setUseSystemLanguageEnabled(context, false, "en");
            assertEquals(tag, AppLanguageSettings.getEffectiveLanguageTag(context, "de-AT"));
            AppLanguageSettings.setUseSystemLanguageEnabled(context, true, "en");
        }
    }
}
