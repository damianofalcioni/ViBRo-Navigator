package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class AboutSettingsScrollRobolectricTest {
    @Test
    public void settingsIntentScrollsToSettingsSection() {
        AboutActivity activity = Robolectric.buildActivity(
                AboutActivity.class,
                AboutActivity.settingsIntent(ApplicationProvider.getApplicationContext())
        ).setup().get();
        ScrollView root = activity.findViewById(R.id.aboutRoot);
        View settingsTitle = activity.findViewById(R.id.aboutSettingsTitle);

        shadowOf(Looper.getMainLooper()).idle();
        root.measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        );
        root.layout(0, 0, 480, 800);
        root.getViewTreeObserver().dispatchOnPreDraw();

        assertTrue(AboutScrollTarget.scrollYFor(root, settingsTitle) > settingsTitle.getTop());
        assertEquals(AboutScrollTarget.scrollYFor(root, settingsTitle), root.getScrollY());
    }
}
