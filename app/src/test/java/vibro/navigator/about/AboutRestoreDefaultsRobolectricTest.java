package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.os.Looper;
import android.widget.Switch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutRestoreDefaultsRobolectricTest {
    @Test
    public void cancelKeepsCustomConfiguration() {
        try (ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            AboutActivity activity = controller.get();
            AppSettings.setImperialUnitsEnabled(activity, true);

            AlertDialog dialog = openConfirmation(activity);
            assertTrue(AppSettings.isImperialUnitsEnabled(activity));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

            assertTrue(AppSettings.isImperialUnitsEnabled(activity));
        }
    }

    @Test
    public void confirmRestoresDefaultsAndPendingSwitchCannotOverwriteThem() {
        try (ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            AboutActivity activity = controller.get();
            activity.getIntent().putExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, true);
            AlertDialog dialog = openConfirmation(activity);
            Switch imperial = activity.findViewById(R.id.aboutImperialUnitsSwitch);
            imperial.setChecked(true);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS);

            assertFalse(AppSettings.isImperialUnitsEnabled(controller.get()));
            Switch restored = controller.get().findViewById(R.id.aboutImperialUnitsSwitch);
            assertFalse(restored.isChecked());
        }
    }

    @Test
    public void confirmRecreatesScreenWithDefaultTheme() {
        try (ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings()) {
            AboutActivity original = controller.get();
            AppThemeSettings.setLightThemeEnabled(original, true);
            controller.recreate();
            AboutActivityTestSupport.idleSettingsInitialization();
            Switch lightTheme = controller.get().findViewById(R.id.aboutLightThemeSwitch);
            assertTrue(lightTheme.isChecked());

            openConfirmation(controller.get()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS);

            assertFalse(AppThemeSettings.isLightThemeEnabled(controller.get()));
            Switch restored = controller.get().findViewById(R.id.aboutLightThemeSwitch);
            assertFalse(restored.isChecked());
        }
    }

    private static AlertDialog openConfirmation(AboutActivity activity) {
        activity.findViewById(R.id.aboutRestoreDefaultsButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(AboutDeferredDialogAction.OPEN_DELAY_MS, TimeUnit.MILLISECONDS);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(dialog.isShowing());
        return dialog;
    }
}
