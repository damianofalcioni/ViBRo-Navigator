package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class BundledProfileInfoTest {

    @Test
    public void forProfile_returnsDescriptionForBundledProfileName() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BundledProfileInfo info = BundledProfileInfo.forProfile("trekking");

        assertEquals(activity.getString(R.string.profile_info_trekking_title),
                activity.getString(info.titleRes()));
        assertFalse(info.hasAttentionIcon());
    }

    @Test
    public void forProfile_returnsDescriptionForCarVarioProfileName() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BundledProfileInfo info = BundledProfileInfo.forProfile("car-vario");

        assertEquals(activity.getString(R.string.profile_info_car_vario_title),
                activity.getString(info.titleRes()));
        assertFalse(info.hasAttentionIcon());
    }

    @Test
    public void forProfile_marksExperimentalAndDebugProfilesForAttentionIcon() {
        assertTrue(BundledProfileInfo.forProfile("moped").hasAttentionIcon());
        assertTrue(BundledProfileInfo.forProfile("dummy").hasAttentionIcon());
    }

    @Test
    public void forProfile_ignoresUnknownProfiles() {
        assertNull(BundledProfileInfo.forProfile("custom-profile"));
    }
}
