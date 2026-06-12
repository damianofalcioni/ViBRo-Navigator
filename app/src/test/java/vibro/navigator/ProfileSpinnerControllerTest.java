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

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ProfileSpinnerControllerTest {

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
        spinner.setSelection(1, false);

        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();

        assertNull(selectedProfile);
        assertFalse(customPickerRequested.get());
        assertTrue(String.valueOf(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.msg_brouter_not_found)));
    }

    @Test
    public void resolveSelectedProfile_allowsStraightLineWithoutBRouter() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                new RepeatSelectSpinner(activity),
                AndroidBRouterProfilesRepositoryFactory.create(),
                () -> {
                }
        );
        controller.refresh();

        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();

        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, selectedProfile.routingMode);
        assertNull(selectedProfile.profileName);
    }

    @Test
    public void selectingSavedCustomProfileAgain_reopensPickerAndKeepsCustomSelection() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        installBRouterPackage(activity);
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        repository.saveCustomProfile(activity, Uri.parse("content://example/custom.brf"), "trekking");
        AtomicBoolean customPickerRequested = new AtomicBoolean(false);
        RepeatSelectSpinner spinner = new RepeatSelectSpinner(activity);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                spinner,
                repository,
                () -> customPickerRequested.set(true)
        );

        controller.refresh();
        spinner.setSelection(1, false);
        customPickerRequested.set(false);
        markSelectionUserInitiated(controller);
        spinner.setSelection(1);

        assertEquals(2, spinner.getCount());
        assertTrue(customPickerRequested.get());
        ProfileSelection selectedProfile = controller.resolveSelectedProfileSelection();
        assertEquals(NavigationRoutingMode.BROUTER, selectedProfile.routingMode);
        assertEquals("trekking", selectedProfile.profileName);
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

        assertEquals(2, spinner.getCount());
        assertEquals(activity.getString(R.string.label_vehicle_profile_straight_line),
                String.valueOf(spinner.getItemAtPosition(0)));
        assertEquals(activity.getString(R.string.label_vehicle_profile_custom),
                String.valueOf(spinner.getItemAtPosition(1)));
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
        public java.util.List<String> listProfiles(Context context) {
            return Collections.emptyList();
        }
    }
}

