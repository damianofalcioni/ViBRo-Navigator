package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ProfileParameterSettingsControllerTest {

    private static final String TREKKING = "trekking";

    @Test
    public void englishBundledProfileHeadingMatchesSpinner() {
        assertProfileHeadings(TREKKING, false);
    }

    @Test
    @Config(qualifiers = "it")
    public void italianBundledProfileHeadingMatchesSpinner() {
        assertProfileHeadings(TREKKING, false);
    }

    @Test
    @Config(qualifiers = "de")
    public void germanBundledProfileHeadingMatchesSpinner() {
        assertProfileHeadings(TREKKING, false);
    }

    @Test
    public void unknownProfileHeadingMatchesSpinner() {
        assertProfileHeadings("my-outdoor-profile", false);
    }

    @Test
    @Config(qualifiers = "it")
    public void customProfileHeadingKeepsCustomSpinnerLabelEvenWhenNameMatchesBundledProfile() {
        assertProfileHeadings(TREKKING, true);
    }

    private static void assertProfileHeadings(String profileName, boolean custom) {
        for (boolean editable : new boolean[]{true, false}) {
            Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
            TestProfilesRepository profiles = new TestProfilesRepository(profileName, editable);
            if (custom) {
                profiles.saveCustomProfile(activity, Uri.parse("content://test/profiles/trekking.brf"), profileName);
            }
            profiles.saveSelectedProfileKey(activity, custom ? ProfileSpinnerOption.CUSTOM_KEY : profileName);
            Spinner spinner = new Spinner(activity);
            ProfileSpinnerController profileController = new ProfileSpinnerController(
                    activity, spinner, profiles, () -> { });
            profileController.refresh();
            String spinnerLabel = spinner.getSelectedItem().toString();
            assertEquals(profileName, profileController.selectedBRouterProfileName());
            ImageButton settingsButton = new ImageButton(activity);
            new ProfileParameterSettingsController(activity, settingsButton, profiles, profileController);

            assertTrue(settingsButton.performClick());

            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(dialog.isShowing());
            assertEquals(activity.getString(R.string.title_profile_parameters, spinnerLabel),
                    org.robolectric.Shadows.shadowOf(dialog).getTitle());
            assertEquals(profileName, profiles.requestedProfile);
            dialog.dismiss();
            activity.finish();
        }
    }

    private static final class TestProfilesRepository extends BRouterProfilesRepository {
        private final String profileName;
        private final boolean editable;
        private String requestedProfile;

        private TestProfilesRepository(String profileName, boolean editable) {
            super(BRouterProfileTestDependencies.create());
            this.profileName = profileName;
            this.editable = editable;
        }

        @Override
        public boolean isBRouterInstalled(Context context) {
            return true;
        }

        @Override
        public List<String> listProfiles(Context context) {
            return Collections.singletonList(profileName);
        }

        @Override
        public List<BRouterProfileParameter> getProfileParameters(Context context, String selectedProfileName) {
            requestedProfile = selectedProfileName;
            return editable ? Collections.singletonList(new BRouterProfileParameter("avoid_paths", "", "0",
                    BRouterProfileParameter.ValueType.BOOLEAN, Collections.emptyList())) : Collections.emptyList();
        }
    }
}
