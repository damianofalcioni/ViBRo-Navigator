package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.util.TypedValue;
import android.widget.ImageButton;

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
            assertNull(stringParameterForm(activity, value).collectValues(activity));
        }
    }

    @Test
    public void textInputAcceptsLiteralPlusPercentAndUtf8() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        String value = "caf\u00e9 + 50% %26%3D%3F";

        assertEquals(Collections.singletonMap("custom", value),
                stringParameterForm(activity, value).collectValues(activity));
    }

    private static ProfileParameterForm stringParameterForm(Activity activity, String value) {
        return ProfileParameterForm.create(
                activity,
                Collections.singletonList(new BRouterProfileParameter(
                        "custom",
                        "",
                        "0",
                        BRouterProfileParameter.ValueType.STRING,
                        Collections.emptyList()
                )),
                Collections.singletonMap("custom", value)
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
