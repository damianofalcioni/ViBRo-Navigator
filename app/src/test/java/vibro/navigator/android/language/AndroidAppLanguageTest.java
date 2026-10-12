package vibro.navigator.android.language;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.widget.TextView;

import java.util.Locale;

import androidx.test.core.app.ApplicationProvider;
import androidx.core.os.ConfigurationCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.R;
import vibro.navigator.about.AboutActivity;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.settings.AppLanguageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28, 33})
public class AndroidAppLanguageTest {
    private static final String ITALIAN_SETTINGS = "Impostazioni";
    private Application application;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
        application.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void matchesFirstSupportedSystemLanguageOrEnglish() {
        assertEquals("de", AndroidAppLanguage.matchSystemLanguages("fr-FR,de-AT,it-IT"));
        assertEquals("it", AndroidAppLanguage.matchSystemLanguages("it-CH,de-DE"));
        assertEquals("en", AndroidAppLanguage.matchSystemLanguages("fr-FR,es-ES"));
    }

    @Test
    public void applicationTextUpdatesImmediatelyAfterManualLanguageChange() {
        manualLanguage("it");
        assertEquals(ITALIAN_SETTINGS, application.getString(R.string.about_settings_title));
        manualLanguage("de");
        assertEquals("Einstellungen", application.getString(R.string.about_settings_title));
    }

    @Test
    public void newActivityUsesSavedLanguageBeforeInflation() {
        manualLanguage("it");
        try (var controller = Robolectric.buildActivity(AboutActivity.class, AboutActivity.settingsIntent(application)).setup()) {
            assertEquals(ITALIAN_SETTINGS, controller.get().getString(R.string.about_settings_title));
            TextView settingsTitle = controller.get().findViewById(R.id.aboutSettingsTitle);
            assertEquals(ITALIAN_SETTINGS, settingsTitle.getText().toString());
            assertEquals("it", ConfigurationCompat.getLocales(controller.get().getResources().getConfiguration())
                    .get(0).getLanguage());
        }
    }

    @Test
    public void existingServiceUsesNewLanguageWithoutRestart() {
        var controller = Robolectric.buildService(NavigationService.class).create();
        try {
            manualLanguage("it");
            assertEquals("Svolta a sinistra", controller.get().getString(R.string.direction_turn_left));
            manualLanguage("de");
            assertEquals("Links abbiegen", controller.get().getString(R.string.direction_turn_left));
            assertEquals("%1$d Sekunden", application.getResources()
                    .getQuantityString(R.plurals.format_time_speech_seconds, 2));
        } finally {
            controller.destroy();
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void systemChangesUpdateAutomaticLanguageButKeepManualSelection() {
        Resources system = Resources.getSystem();
        Configuration original = new Configuration(system.getConfiguration());
        try {
            Configuration changed = new Configuration(original);
            changed.setLocale(Locale.ITALIAN);
            system.updateConfiguration(changed, system.getDisplayMetrics());
            assertEquals(ITALIAN_SETTINGS, application.getString(R.string.about_settings_title));
            manualLanguage("de");
            changed.setLocale(Locale.ENGLISH);
            system.updateConfiguration(changed, system.getDisplayMetrics());
            assertEquals("Einstellungen", application.getString(R.string.about_settings_title));
            AppLanguageSettings.setUseSystemLanguageEnabled(application, true, AndroidAppLanguage.systemLanguageTag());
            assertEquals("Settings", application.getString(R.string.about_settings_title));
        } finally {
            system.updateConfiguration(original, system.getDisplayMetrics());
        }
    }

    @Test
    public void returningActivityRecreatesInTheNewLanguage() {
        manualLanguage("de");
        try (var controller = Robolectric.buildActivity(AboutActivity.class).setup()) {
            controller.pause().stop();
            manualLanguage("it");
            controller.start().resume().visible();
            assertEquals(ITALIAN_SETTINGS, controller.get().getString(R.string.about_settings_title));
        }
    }

    private void manualLanguage(String language) {
        AppLanguageSettings.setSelectedLanguageTag(application, language);
        AppLanguageSettings.setUseSystemLanguageEnabled(application, false, AndroidAppLanguage.systemLanguageTag());
    }
}
