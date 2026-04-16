package vibro.navigator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.widget.Spinner;

import vibro.navigator.brouter.BRouterProfilesRepository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
public class ProfileSpinnerControllerTest {

    @Test
    public void shouldPromptForProfilesFolder_returnsFalseWhenBRouterNotInstalled() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                new Spinner(activity),
                new BRouterProfilesRepository(),
                () -> {
                }
        );

        controller.refresh();

        assertFalse(controller.shouldPromptForProfilesFolder());
    }

    @Test
    public void resolveSelectedProfile_showsMissingBRouterToastInsteadOfOpeningCustomPicker() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AtomicBoolean customPickerRequested = new AtomicBoolean(false);
        ProfileSpinnerController controller = new ProfileSpinnerController(
                activity,
                new Spinner(activity),
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
}
