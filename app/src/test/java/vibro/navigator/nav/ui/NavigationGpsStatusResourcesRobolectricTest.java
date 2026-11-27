package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.presentation.NavStateComposer;

@RunWith(RobolectricTestRunner.class)
public class NavigationGpsStatusResourcesRobolectricTest {
    @Test
    public void refreshingTheGpsLineReusesTheLocalizedConfigurationContext() {
        try (var controller = Robolectric.buildActivity(CountingActivity.class).setup()) {
            var activity = controller.get();
            var dialogs = new NavigationDetailsDialogs(activity, () -> 10_000);
            var state = NavStateComposer.waiting(activity);
            int initial = activity.configurationContextCount;
            String line = dialogs.gpsStatusLine(state);
            for (int frame = 0; frame < 40; frame++) {
                assertEquals(line, dialogs.gpsStatusLine(state));
            }
            assertEquals(initial + 1, activity.configurationContextCount);
        }
    }

    public static class CountingActivity extends Activity {
        int configurationContextCount;

        @Override
        public Context createConfigurationContext(Configuration configuration) {
            configurationContextCount++;
            return super.createConfigurationContext(configuration);
        }
    }
}
