package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.map.MapPickerActivity;
import vibro.navigator.settings.AppSimulationSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class AboutSimulationMapTest {
    @Test
    public void mapSelectionSurvivesActivityRecreationWithoutDiscardingUnsavedSpeed() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = AboutSimulationSettingsTest.open(activity);
        ((EditText) dialog.findViewById(R.id.simulationSpeed)).setText("36");
        ((EditText) dialog.findViewById(R.id.simulationPosition)).setText("48.2, 16.3");
        dialog.findViewById(R.id.simulationMapButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(110, TimeUnit.MILLISECONDS);
        ShadowActivity.IntentForResult picker = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(MapPickerActivity.class.getName(), picker.intent.getComponent().getClassName());
        assertEquals(48.2, picker.intent.getDoubleExtra("initial_lat", 0), 0);
        Bundle saved = new Bundle();
        activity.onSaveInstanceState(saved);
        AboutActivity restored = Robolectric.buildActivity(AboutActivity.class).create(saved).start().resume().get();
        restored.onActivityResult(picker.requestCode, Activity.RESULT_OK,
                new Intent().putExtra("result_lat", 47.5).putExtra("result_lon", 15.4));
        AlertDialog reopened = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(reopened.isShowing());
        assertEquals("36", ((EditText) reopened.findViewById(R.id.simulationSpeed)).getText().toString());
        assertEquals("47.500000, 15.400000", ((EditText) reopened.findViewById(R.id.simulationPosition)).getText().toString());
        reopened.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(110, TimeUnit.MILLISECONDS);
        assertEquals(10, AppSimulationSettings.read(restored).speedMps, 0.01);
    }
}
