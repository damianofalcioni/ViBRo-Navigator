package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.settings.AppLanguageSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutLanguageSettingsRobolectricTest {
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void defaultsEnabledAndConfigurationRemainsAvailable() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch systemSwitch = activity.findViewById(R.id.aboutUseSystemLanguageSwitch);
        assertTrue(systemSwitch.isChecked());
        TextView label = activity.findViewById(R.id.aboutLanguageLabel);
        assertEquals(activity.getString(R.string.label_use_system_language), label.getText().toString());
        assertEquals(label.getText().toString(), systemSwitch.getContentDescription().toString());
        assertTrue(activity.findViewById(R.id.aboutLanguageSettingsButton).isEnabled());
        Spinner spinner = openLanguageDialog(activity).findViewById(R.id.aboutLanguageSpinner);
        String[] tags = AppLanguageSettings.availableLanguageTags();
        assertEquals(AndroidAppLanguage.systemLanguageTag(), tags[spinner.getSelectedItemPosition()]);
        assertEquals(3, spinner.getCount());
    }

    @Test
    public void dialogSavesManualChoiceWithoutChangingAutomaticMode() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = openLanguageDialog(activity);
        Spinner spinner = dialog.findViewById(R.id.aboutLanguageSpinner);
        spinner.setSelection(1);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(AppLanguageSettings.isUseSystemLanguageEnabled(activity));
        assertEquals("it", AppLanguageSettings.getSelectedLanguageTag(activity, "en"));
        ((Switch) activity.findViewById(R.id.aboutUseSystemLanguageSwitch)).performClick();
        assertFalse(AppLanguageSettings.isUseSystemLanguageEnabled(activity));
        assertEquals("it", AppLanguageSettings.getEffectiveLanguageTag(activity, "en"));
    }

    @Test
    public void cancellingDialogKeepsPreviouslySavedLanguage() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AppLanguageSettings.setSelectedLanguageTag(activity, "de");
        AlertDialog dialog = openLanguageDialog(activity);
        ((Spinner) dialog.findViewById(R.id.aboutLanguageSpinner)).setSelection(1);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("de", AppLanguageSettings.getSelectedLanguageTag(activity, "en"));
    }

    private AlertDialog openLanguageDialog(AboutActivity activity) {
        activity.findViewById(R.id.aboutLanguageSettingsButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS + 50, TimeUnit.MILLISECONDS);
        return ShadowAlertDialog.getLatestAlertDialog();
    }
}
