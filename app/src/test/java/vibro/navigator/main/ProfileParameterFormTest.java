package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
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
