package vibro.navigator.main;

import vibro.navigator.R;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.model.NavigationRoutingMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.ShadowPackageManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ProfileSpinnerControllerTest {
    private static final String PROFILE_TREKKING = "trekking";
    private static final String PROFILE_FASTBIKE = "fastbike";

    @Test
    public void resolveSelectedProfile_showsMissingBRouterToastInsteadOfOpeningCustomPicker() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AtomicBoolean customPickerRequested = new AtomicBoolean(false);
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                AndroidBRouterProfilesRepositoryFactory.create(),
                () -> customPickerRequested.set(true)
        );
        controller.refresh();
        spinner.setSelection(0, false);

        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();

        assertNull(selectedProfile);
        assertFalse(customPickerRequested.get());
        assertTrue(String.valueOf(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.msg_brouter_not_found)));
    }

    @Test
    public void selectingSavedCustomProfileAgain_reopensPickerAndKeepsCustomSelection() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        installBRouterPackage(activity);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        repository.saveCustomProfile(activity, Uri.parse("content://example/custom.brf"), PROFILE_TREKKING);
        AtomicBoolean customPickerRequested = new AtomicBoolean(false);
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                repository,
                () -> customPickerRequested.set(true)
        );

        controller.refresh();
        spinner.setSelection(0, false);
        customPickerRequested.set(false);
        markSelectionUserInitiated(controller);
        spinner.setSelection(0);

        assertEquals(1, spinner.getCount());
        assertTrue(customPickerRequested.get());
        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();
        assertEquals(NavigationRoutingMode.BROUTER, selectedProfile.routingMode);
        assertEquals(PROFILE_TREKKING, selectedProfile.profileName);
        assertTrue(selectedProfile.customProfile);
    }

    @Test
    public void refresh_keepsOnlyCustomOptionWhenNoExternalOrBundledProfilesExist() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        installBRouterPackage(activity);
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                new EmptyProfilesRepository(),
                () -> {
                }
        );

        controller.refresh();

        assertEquals(1, spinner.getCount());
        assertEquals(activity.getString(R.string.label_vehicle_profile_custom),
                String.valueOf(spinner.getItemAtPosition(0)));
    }

    @Test
    public void refresh_listsBRouterProfilesAndCustomEntryButDefaultsToBRouterWhenInstalled() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                new ProfilesRepository(true, Arrays.asList(PROFILE_TREKKING, PROFILE_FASTBIKE)),
                () -> {
                }
        );

        controller.refresh();

        assertEquals(3, spinner.getCount());
        assertEquals(PROFILE_TREKKING, String.valueOf(spinner.getItemAtPosition(0)));
        assertEquals(PROFILE_FASTBIKE, String.valueOf(spinner.getItemAtPosition(1)));
        assertEquals(0, spinner.getSelectedItemPosition());
        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();
        assertEquals(NavigationRoutingMode.BROUTER, selectedProfile.routingMode);
        assertEquals(PROFILE_TREKKING, selectedProfile.profileName);
        assertFalse(selectedProfile.customProfile);
    }

    @Test
    public void refresh_defaultsToCustomWhenBRouterIsNotInstalled() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                new ProfilesRepository(false, Arrays.asList(PROFILE_TREKKING)),
                () -> {
                }
        );

        controller.refresh();

        assertEquals(1, spinner.getSelectedItemPosition());
        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();
        assertNull(selectedProfile);
        assertTrue(String.valueOf(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.msg_brouter_not_found)));
    }

    @Test
    public void resolveSelectedProfile_includesSavedProfileParameterOverrides() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfilesRepository repository = new ProfilesRepository(true, Collections.singletonList(PROFILE_TREKKING));
        Map<String, String> values = new HashMap<>();
        values.put("avoid_path", "1");
        repository.saveProfileParameterValues(
                activity,
                PROFILE_TREKKING,
                Collections.singletonList(new BRouterProfileParameter(
                        "avoid_path",
                        "Avoid paths",
                        "0",
                        BRouterProfileParameter.ValueType.BOOLEAN,
                        null
                )),
                values
        );
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                new RepeatSelectSpinner(activity),
                repository,
                () -> {
                }
        );

        controller.refresh();
        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();

        assertEquals(PROFILE_TREKKING, selectedProfile.profileName);
        assertFalse(selectedProfile.customProfile);
        assertEquals("avoid_path=1", selectedProfile.profileParameters);
    }

    @Test
    public void profileSettingsButton_disablesWhenProfileDoesNotApplyToCurrentMode() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfilesRepository repository = new ProfilesRepository(true, Collections.singletonList(PROFILE_TREKKING));
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                repository,
                () -> {
                }
        );
        ImageButton settingsButton = new ImageButton(activity);
        ProfileParameterSettingsController settingsController = new ProfileParameterSettingsController(
                activity,
                settingsButton,
                repository,
                controller
        );
        controller.setSelectionChangeListener(settingsController::updateButtonState);

        controller.refresh();
        assertTrue(settingsButton.isEnabled());

        settingsController.setProfileSelectionApplicable(false);
        assertFalse(settingsButton.isEnabled());

        settingsController.setProfileSelectionApplicable(true);
        assertTrue(settingsButton.isEnabled());
    }

    private static void installBRouterPackage(Activity activity) {
        ShadowPackageManager shadowPackageManager = shadowOf(activity.getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowPackageManager.installPackage(packageInfo);
    }

    private static void markSelectionUserInitiated(ProfileSpinnerController controller) throws Exception {
        Field field = ProfileSpinnerController.class.getDeclaredField("selectionUserInitiated");
        field.setAccessible(true);
        field.setBoolean(controller, true);
    }

    private static final class EmptyProfilesRepository extends BRouterProfilesRepository {
        private EmptyProfilesRepository() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public boolean isBRouterInstalled(Context context) {
            return true;
        }

        @Override
        public List<String> listProfiles(Context context) {
            return Collections.emptyList();
        }
    }

    private static final class ProfilesRepository extends BRouterProfilesRepository {
        private final boolean bRouterInstalled;
        @NonNull
        private final List<String> profiles;

        private ProfilesRepository(boolean bRouterInstalled, @NonNull List<String> profiles) {
            super(BRouterProfileTestDependencies.create());
            this.bRouterInstalled = bRouterInstalled;
            this.profiles = profiles;
        }

        @Override
        public boolean isBRouterInstalled(Context context) {
            return bRouterInstalled;
        }

        @Override
        public String getSelectedProfileKey(Context context) {
            return null;
        }

        @Override
        public void saveSelectedProfileKey(Context context, String selectionKey) {
        }

        @Override
        public String getCustomProfileName(Context context) {
            return null;
        }

        @Override
        public List<String> listProfiles(Context context) {
            return profiles;
        }
    }
}

