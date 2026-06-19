package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;

import vibro.navigator.R;

final class AboutDialogButtonStyleAssertions {
    private AboutDialogButtonStyleAssertions() {
    }

    static void assertBorderlessProfileInfoBackground(Activity activity, View button) {
        assertTrue(button.getBackgroundTintList() == null);
        assertEquals(borderlessSelectableBackground(activity), shadowOf(button.getBackground()).getCreatedFromResId());
    }

    static void assertIconButtonBackground(View button) {
        assertTrue(button.getBackgroundTintList() == null);
        assertEquals(R.drawable.bg_button_icon, shadowOf(button.getBackground()).getCreatedFromResId());
    }

    static void assertSecondaryButtonBackground(View button) {
        assertTrue(button.getBackgroundTintList() == null);
        assertEquals(R.drawable.bg_button_secondary, shadowOf(button.getBackground()).getCreatedFromResId());
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
