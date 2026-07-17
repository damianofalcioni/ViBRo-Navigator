package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.content.ServiceConnection;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class NavigationExportButtonLayoutRobolectricTest {
    @Test
    public void exportButtonIsBottomRightOfCompassInPortrait() {
        assertExportButtonIsBottomRightOfCompass();
    }

    @Test
    @Config(qualifiers = "land")
    public void exportButtonIsBottomRightOfCompassInLandscape() {
        assertExportButtonIsBottomRightOfCompass();
    }

    private static void assertExportButtonIsBottomRightOfCompass() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        View button = controller.get().findViewById(R.id.exportRouteButton);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();

        assertEquals(0, params.getRule(RelativeLayout.ALIGN_TOP));
        assertEquals(R.id.navigationCompassView, params.getRule(RelativeLayout.ALIGN_BOTTOM));
        assertEquals(R.id.navigationCompassView, resolvedEndRule(params));
    }

    private static int resolvedEndRule(RelativeLayout.LayoutParams params) {
        int alignEndRule = params.getRule(RelativeLayout.ALIGN_END);
        return alignEndRule == 0 ? params.getRule(RelativeLayout.ALIGN_RIGHT) : alignEndRule;
    }

    public static class TestNavigationActivity extends NavigationActivity {
        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return false;
        }
    }
}
