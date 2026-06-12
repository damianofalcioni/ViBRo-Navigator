package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.R;

public class BundledProfileInfoTest {

    @Test
    public void forProfile_returnsDescriptionForBundledProfileName() {
        BundledProfileInfo info = BundledProfileInfo.forProfile("trekking");

        assertEquals(R.string.profile_info_trekking_title, info.titleRes());
        assertFalse(info.hasAttentionIcon());
    }

    @Test
    public void forProfile_returnsDescriptionForCarVarioProfileName() {
        BundledProfileInfo info = BundledProfileInfo.forProfile("car-vario");

        assertEquals(R.string.profile_info_car_vario_title, info.titleRes());
        assertFalse(info.hasAttentionIcon());
    }

    @Test
    public void forProfile_marksExperimentalAndDebugProfilesForAttentionIcon() {
        assertTrue(BundledProfileInfo.forProfile("moped").hasAttentionIcon());
        assertTrue(BundledProfileInfo.forProfile("dummy").hasAttentionIcon());
    }

    @Test
    public void straightLine_returnsDirectGuidanceDescriptionWithoutAttentionIcon() {
        BundledProfileInfo info = BundledProfileInfo.straightLine();

        assertEquals(R.string.profile_info_straight_line_title, info.titleRes());
        assertFalse(info.hasAttentionIcon());
    }

    @Test
    public void forProfile_ignoresUnknownProfiles() {
        assertNull(BundledProfileInfo.forProfile("custom-profile"));
    }
}
