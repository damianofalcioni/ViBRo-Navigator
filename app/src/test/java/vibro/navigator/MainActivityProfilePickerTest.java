package vibro.navigator;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.DocumentsContract;

import vibro.navigator.brouter.BRouterProfilesRepository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class MainActivityProfilePickerTest {

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void startCustomProfilePicker_usesFallbackInitialUriBeforeAndroid11() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainActivityProfilePicker picker = new MainActivityProfilePicker(
                activity,
                new BRouterProfilesRepository()
        );

        picker.startCustomProfilePicker();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent intent = started.intent;
        assertTrue(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void startCustomProfilePicker_usesFallbackInitialUriOnAndroid11AndLater() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainActivityProfilePicker picker = new MainActivityProfilePicker(
                activity,
                new BRouterProfilesRepository()
        );

        picker.startCustomProfilePicker();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent intent = started.intent;
        assertTrue(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }
}
