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

import vibro.navigator.brouter.BRouterProfilesRepository;

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
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                new RepeatSelectSpinner(activity),
                new BRouterProfilesRepository(),
                () -> customPickerRequested.set(true)
        );
        controller.refresh();

        String selectedProfile = controller.resolveSelectedProfile();

        assertNull(selectedProfile);
        assertFalse(customPickerRequested.get());
        assertTrue(String.valueOf(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.msg_brouter_not_found)));
    }

    @Test
    public void selectingSavedCustomProfileAgain_reopensPickerAndKeepsCustomSelection() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        installBRouterPackage(activity);
        BRouterProfilesRepository repository = new BRouterProfilesRepository();
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
        spinner.setSelection(0, false);
        customPickerRequested.set(false);
        markSelectionUserInitiated(controller);
        spinner.setSelection(0);

        assertEquals(1, spinner.getCount());
        assertTrue(customPickerRequested.get());
        assertEquals("trekking", controller.resolveSelectedProfile());
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

