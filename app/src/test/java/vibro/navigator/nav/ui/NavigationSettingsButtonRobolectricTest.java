package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Looper;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.about.AboutActivity;

@RunWith(RobolectricTestRunner.class)
public class NavigationSettingsButtonRobolectricTest {
    @Test
    public void settingsButtonOpensAboutPageAtSettingsSection() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TestNavigationActivity.class);
        intent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        ActivityController<TestNavigationActivity> controller =
                Robolectric.buildActivity(TestNavigationActivity.class, intent).setup();
        TestNavigationActivity activity = controller.get();
        View settingsButton = activity.findViewById(R.id.navigationSettingsButton);

        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertEquals(AboutActivity.class.getName(), started.getComponent().getClassName());
        assertTrue(started.getBooleanExtra(AboutActivity.EXTRA_SCROLL_TO_SETTINGS, false));
    }

    public static class TestNavigationActivity extends NavigationActivity {
        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return false;
        }
    }
}
