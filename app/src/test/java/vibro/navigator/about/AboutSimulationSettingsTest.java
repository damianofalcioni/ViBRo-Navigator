package vibro.navigator.about;

import android.app.AlertDialog;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Switch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.settings.AppSimulationSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class AboutSimulationSettingsTest {
    @Test
    public void destroyingActivityClosesItsConfigurationWindow() {
        var controller = AboutActivityTestSupport.setupControllerWithSettings();
        AlertDialog dialog = open(controller.get());
        controller.pause().stop().destroy();
        assertFalse(dialog.isShowing());
    }

    @Test
    public void switchDefersPersistenceButFlushesBeforeLeavingSettings() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch toggle = activity.findViewById(R.id.aboutSimulationSwitch);
        assertFalse(toggle.isChecked());
        toggle.setChecked(true);
        assertFalse(AppSimulationSettings.isEnabled(activity));
        activity.flushDeferredSettings();
        assertTrue(AppSimulationSettings.isEnabled(activity));
    }

    @Test
    public void configValidatesWithoutClosingThenSavesSpeedAndOptionalOrigin() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = open(activity);
        EditText speed = dialog.findViewById(R.id.simulationSpeed);
        EditText position = dialog.findViewById(R.id.simulationPosition);
        speed.setText("999");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(110, TimeUnit.MILLISECONDS);
        assertTrue(dialog.isShowing());
        assertNotNull(speed.getError());
        speed.setText("36");
        position.setText("48.2, 16.3");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(110, TimeUnit.MILLISECONDS);
        assertFalse(dialog.isShowing());
        assertEquals(10, AppSimulationSettings.read(activity).speedMps, 0.01);
        assertEquals(48.2, AppSimulationSettings.read(activity).initialPosition.lat, 0);
        assertFalse(AppSimulationSettings.isEnabled(activity));
    }

    @Test
    public void cancellingConfigurationDoesNotPersistDraft() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = open(activity);
        ((EditText) dialog.findViewById(R.id.simulationSpeed)).setText("36");
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        assertEquals(50 / 3.6, AppSimulationSettings.read(activity).speedMps, 0.01);
    }

    static AlertDialog open(AboutActivity activity) {
        activity.findViewById(R.id.aboutSimulationConfigButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(110, TimeUnit.MILLISECONDS);
        return ShadowAlertDialog.getLatestAlertDialog();
    }
}
