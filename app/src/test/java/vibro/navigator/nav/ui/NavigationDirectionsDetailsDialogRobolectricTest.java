package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

@RunWith(RobolectricTestRunner.class)
public class NavigationDirectionsDetailsDialogRobolectricTest {

    @Test
    public void updateBeforeShowDoesNotBuildDirectionsList() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class).setup();
        CountingDirectionsProvider provider = new CountingDirectionsProvider();

        new NavigationDirectionsDetailsDialog(controller.get()).update(provider);

        assertEquals(0, provider.calls);
    }

    @Test
    public void showDisplaysLiveDirectionsList() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class).setup();
        Activity activity = controller.get();
        CountingDirectionsProvider provider = new CountingDirectionsProvider();

        new NavigationDirectionsDetailsDialog(activity).show(provider);

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        View content = dialog.getWindow().getDecorView();
        assertEquals(1, provider.calls);
        assertTrue(hasText(content, activity.getString(R.string.title_nav_directions_details)));
        assertTrue(hasText(content, "1. First live direction"));
        assertTrue(hasText(content, "2. Second live direction"));
    }

    private static boolean hasText(@Nullable View view, @NonNull String expected) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(expected)) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (hasText(group.getChildAt(i), expected)) {
                return true;
            }
        }
        return false;
    }

    private static final class CountingDirectionsProvider
            implements NavigationDirectionsDetailsDialog.DirectionDetailsProvider {
        private int calls;

        @Override
        @NonNull
        public List<String> buildCurrentDirectionDetails() {
            calls++;
            return Arrays.asList("First live direction", "Second live direction");
        }
    }

    public static class TestActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            AndroidAppTheme.apply(this);
            super.onCreate(savedInstanceState);
        }
    }
}
