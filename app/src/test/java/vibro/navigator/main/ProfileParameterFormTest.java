package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfileParameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class ProfileParameterFormTest {
    private static final String CUSTOM = "custom";

    @Test
    public void profileInfoButtonUsesProfileSpinnerInfoBackground() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileParameterForm form = ProfileParameterForm.create(
                activity,
                Collections.singletonList(new BRouterProfileParameter(
                        "avoid_path",
                        "Avoid paths",
                        "0",
                        BRouterProfileParameter.ValueType.BOOLEAN,
                        Collections.emptyList()
                )),
                Collections.emptyMap()
        );

        ImageButton infoButton = form.view().findViewById(R.id.profileParameterInfoButton);

        assertTrue(infoButton.getBackgroundTintList() == null);
        assertEquals(borderlessSelectableBackground(activity),
                shadowOf(infoButton.getBackground()).getCreatedFromResId());
    }

    @Test
    public void textInputRejectsBRouterSeparators() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        for (String value : Arrays.asList("a?b", "a&b", "a=b")) {
            ProfileParameterForm form = stringParameterForm(activity);
            setText(form, value);
            assertNull(form.collectChanges(activity));
        }
    }

    @Test
    public void textInputAcceptsLiteralPlusPercentAndUtf8() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String value = "caf\u00e9 + 50% %26%3D%3F";
        ProfileParameterForm form = stringParameterForm(activity);
        setText(form, value);

        assertEquals(Collections.singletonMap(CUSTOM, value),
                form.collectChanges(activity));
    }

    @Test
    public void onlyEditsAndExplicitResetsSubmitValues() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileParameterForm form = stringParameterForm(activity);
        assertTrue(form.collectChanges(activity).isEmpty());
        assertEquals(Collections.singletonMap(CUSTOM, "0"), form.resetToDefaults());
        assertTrue(form.collectChanges(activity).isEmpty());
    }

    private static void setText(ProfileParameterForm form, String value) {
        LinearLayout rows = form.view().findViewById(R.id.profileParameterRows);
        LinearLayout row = (LinearLayout) rows.getChildAt(rows.getChildCount() - 1);
        ((EditText) row.getChildAt(1)).setText(value);
    }

    private static ProfileParameterForm stringParameterForm(Activity activity) {
        return ProfileParameterForm.create(
                activity,
                Collections.singletonList(new BRouterProfileParameter(
                        CUSTOM,
                        "",
                        "0",
                        BRouterProfileParameter.ValueType.STRING,
                        Collections.emptyList()
                )),
                Collections.singletonMap(CUSTOM, "saved")
        );
    }

    private static int borderlessSelectableBackground(Activity activity) {
        TypedValue value = new TypedValue();
        assertTrue(activity.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                value,
                true
        ));
        return value.resourceId;
    }
}
