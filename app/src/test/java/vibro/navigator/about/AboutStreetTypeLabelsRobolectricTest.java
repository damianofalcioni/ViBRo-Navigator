package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;

import java.time.Duration;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetVisibility;
import vibro.navigator.settings.AppCompassStreetTypeSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutStreetTypeLabelsRobolectricTest {
    @Test
    public void everySelectableTypeHasPlainLabelDescriptionAndInfoButton() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog selector = open(activity);

        for (CompassStreetType type : CompassStreetType.values()) {
            if (type == CompassStreetType.ROUTE_WALKING_CYCLING
                    || type == CompassStreetType.SPECIAL_ROUTE) {
                continue;
            }
            Switch control = (Switch) findView(selector.getWindow().getDecorView(), type.name());
            assertNotNull(type.name(), control);
            String label = control.getText().toString();
            assertEquals(AboutStreetTypeLabels.typeLabel(activity, type), label);
            assertFalse(type.name(), label.contains("="));
            String description = AboutStreetTypeLabels.typeDescription(activity, type);
            assertTrue(type.name(), description.contains("\n\nOSM/BRouter:"));
            ImageButton info = (ImageButton) findView(selector.getWindow().getDecorView(),
                    type.name() + "_INFO");
            assertNotNull(type.name(), info);
            assertEquals(activity.getString(R.string.format_about_setting_info_content_description, label),
                    info.getContentDescription().toString());
        }

        assertTag(activity, CompassStreetType.TRUNK, "highway=trunk");
        assertTag(activity, CompassStreetType.ROAD, "highway=road or highway=yes");
        assertTag(activity, CompassStreetType.RAILWAY, "railway=*");
        assertTag(activity, CompassStreetType.WATERWAY, "waterway=*");
        assertTag(activity, CompassStreetType.ROUTE_HIKING_FOOT, "route=hiking or route=foot");
        assertTag(activity, CompassStreetType.ROUTE_SKI_PISTE, "route=ski or route=piste");
        assertTag(activity, CompassStreetType.OTHER, "no recognized highway=*");
    }

    @Test
    public void infoOpensDescriptionWithoutDiscardingUnsavedSelection() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog selector = open(activity);
        Switch trunk = (Switch) findView(selector.getWindow().getDecorView(), CompassStreetType.TRUNK.name());
        assertNotNull(trunk);
        trunk.performClick();

        View info = findView(selector.getWindow().getDecorView(), CompassStreetType.TRUNK.name() + "_INFO");
        assertNotNull(info);
        info.performClick();
        AlertDialog help = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(help);
        assertTrue(help.isShowing());
        TextView message = help.findViewById(android.R.id.message);
        assertEquals(AboutStreetTypeLabels.typeDescription(activity, CompassStreetType.TRUNK),
                message.getText().toString());
        help.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        assertTrue(selector.isShowing());
        assertFalse(trunk.isChecked());
        selector.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertFalse(AppCompassStreetTypeSettings.get(activity).isTypeEnabled(CompassStreetType.TRUNK));
    }

    private static void assertTag(AboutActivity activity, CompassStreetType type, String tag) {
        assertTrue(type.name(), AboutStreetTypeLabels.typeDescription(activity, type).contains(tag));
    }

    private static AlertDialog open(AboutActivity activity) {
        AppCompassStreetTypeSettings.set(activity, CompassStreetVisibility.all());
        activity.findViewById(R.id.aboutCompassSurroundingStreetsSettingsButton).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(150));
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        return dialog;
    }

    private static View findView(View root, String tag) {
        if (tag.equals(root.getTag())) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findView(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
