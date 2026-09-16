package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;

import java.time.Duration;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetVisibility;
import vibro.navigator.settings.AppCompassStreetTypeSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutSurroundingStreetTypesRobolectricTest {
    @Test
    public void settingsButtonSitsImmediatelyLeftOfMasterSwitch() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        LinearLayout row = activity.findViewById(R.id.aboutCompassSurroundingStreetsRow);

        assertSame(activity.findViewById(R.id.aboutCompassSurroundingStreetsLabel), row.getChildAt(0));
        assertSame(activity.findViewById(R.id.aboutCompassSurroundingStreetsSettingsButton), row.getChildAt(1));
        assertSame(activity.findViewById(R.id.aboutCompassSurroundingStreetsSwitch), row.getChildAt(2));
        assertSame(activity.findViewById(R.id.aboutCompassSurroundingStreetsInfoButton), row.getChildAt(3));
    }

    @Test
    public void pickerShowsFourColorGroupsAndSeparateRailwayWaterwayAndRoutes() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = open(activity);
        Switch railway = findSwitch(dialog, CompassStreetType.RAILWAY.name());
        for (CompassStreetCategory category : CompassStreetCategory.values()) {
            Switch categorySwitch = findSwitch(dialog, category.name());
            assertTrue(categorySwitch.isChecked());
            LinearLayout header = (LinearLayout) categorySwitch.getParent();
            assertEquals(3, header.getChildCount());
            TextView heading = (TextView) header.getChildAt(0);
            TextView swatch = (TextView) header.getChildAt(1);
            assertSame(categorySwitch, header.getChildAt(2));
            assertEquals(activity.getString(AboutStreetTypeLabels.categoryLabel(category)),
                    heading.getText().toString());
            assertEquals(activity.getString(R.string.street_color_swatch), swatch.getText().toString());
            assertTrue((heading.getTypeface().getStyle() & Typeface.BOLD) != 0);
            assertTrue(heading.getTextSize() > railway.getTextSize());
        }
        for (CompassStreetType type : CompassStreetType.values()) {
            if (type != CompassStreetType.ROUTE_WALKING_CYCLING
                    && type != CompassStreetType.SPECIAL_ROUTE) {
                assertTrue(findSwitch(dialog, type.name()).isChecked());
            }
        }
        assertEquals(activity.getString(R.string.street_type_railway),
                railway.getText().toString());
        assertEquals(activity.getString(R.string.street_type_waterway),
                findSwitch(dialog, CompassStreetType.WATERWAY.name()).getText().toString());
        assertEquals("Major roads", activity.getString(R.string.street_category_highway));
        assertEquals("Normal streets", activity.getString(R.string.street_category_normal));
        assertEquals("Walking / Cycling", activity.getString(R.string.street_category_walking_cycling));
        assertEquals("Special paths", activity.getString(R.string.street_category_special_routing));
    }

    @Test
    public void routeEntriesComeFirstAndUnknownGeometryComesLast() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = open(activity);
        LinearLayout content = (LinearLayout) findSwitch(dialog, CompassStreetCategory.SPECIAL_ROUTING.name())
                .getParent().getParent();

        assertRoutesBeforeHighways(dialog, content, CompassStreetCategory.WALKING_CYCLING,
                CompassStreetType.LIVING_STREET);
        assertRoutesBeforeHighways(dialog, content, CompassStreetCategory.SPECIAL_ROUTING,
                CompassStreetType.BUSWAY);
        int otherIndex = content.indexOfChild(rowOf(dialog, CompassStreetType.OTHER));
        assertEquals(content.getChildCount() - 1, otherIndex);
    }

    private static void assertRoutesBeforeHighways(
            AlertDialog dialog,
            LinearLayout content,
            CompassStreetCategory category,
            CompassStreetType firstHighway
    ) {
        int highwayIndex = content.indexOfChild(rowOf(dialog, firstHighway));
        int headingIndex = content.indexOfChild((View) findSwitch(dialog, category.name()).getParent());
        for (CompassStreetType type : CompassStreetType.values()) {
            if (type.category() == category && type.name().startsWith("ROUTE_")
                    && type != CompassStreetType.ROUTE_WALKING_CYCLING) {
                int routeIndex = content.indexOfChild(rowOf(dialog, type));
                assertTrue(type.name(), routeIndex > headingIndex && routeIndex < highwayIndex);
            }
        }
    }

    private static View rowOf(AlertDialog dialog, CompassStreetType type) {
        return (View) findSwitch(dialog, type.name()).getParent();
    }

    @Test
    public void saveKeepsGroupAndLeafSelectionsSeparate() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AlertDialog dialog = open(activity);
        Switch group = findSwitch(dialog, CompassStreetCategory.SPECIAL_ROUTING.name());
        assertTrue(group.isChecked());
        group.performClick();
        assertFalse(group.isChecked());
        Switch railway = findSwitch(dialog, CompassStreetType.RAILWAY.name());
        assertTrue(railway.isChecked());
        railway.performClick();
        assertFalse(railway.isChecked());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        CompassStreetVisibility saved = AppCompassStreetTypeSettings.get(activity);
        assertFalse(saved.isCategoryEnabled(CompassStreetCategory.SPECIAL_ROUTING));
        assertFalse(saved.isTypeEnabled(CompassStreetType.RAILWAY));
        assertTrue(saved.isTypeEnabled(CompassStreetType.WATERWAY));
        assertTrue(saved.isVisible(CompassStreetType.ROUTE_HIKING_FOOT));
    }

    private static AlertDialog open(AboutActivity activity) {
        AppCompassStreetTypeSettings.set(activity, CompassStreetVisibility.all());
        View button = activity.findViewById(R.id.aboutCompassSurroundingStreetsSettingsButton);
        assertNotNull(button);
        button.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(150));
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        return dialog;
    }

    private static Switch findSwitch(AlertDialog dialog, String tag) {
        Switch result = findSwitch(dialog.getWindow().getDecorView(), tag);
        assertNotNull(tag, result);
        return result;
    }

    private static Switch findSwitch(View root, String tag) {
        if (root instanceof Switch && tag.equals(root.getTag())) {
            return (Switch) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                Switch found = findSwitch(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

}
