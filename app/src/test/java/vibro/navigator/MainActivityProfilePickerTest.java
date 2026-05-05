package vibro.navigator.main;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
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
    public void startCustomProfilePicker_requestsProfilesTreeBeforeAndroid11WhenNoTreeGrantExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainActivityProfilePicker picker = new MainActivityProfilePicker(
                activity,
                new TestProfilesRepository(false)
        );

        picker.startCustomProfilePicker();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent intent = started.intent;
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, intent.getAction());
        assertTrue(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void startCustomProfilePicker_requestsProfilesTreeOnAndroid11AndLaterWhenNoTreeGrantExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainActivityProfilePicker picker = new MainActivityProfilePicker(
                activity,
                new TestProfilesRepository(false)
        );

        picker.startCustomProfilePicker();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent intent = started.intent;
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, intent.getAction());
        assertTrue(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void startCustomProfilePicker_opensDocumentPickerWhenTreeGrantAlreadyExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainActivityProfilePicker picker = new MainActivityProfilePicker(
                activity,
                new TestProfilesRepository(true)
        );

        picker.startCustomProfilePicker();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent intent = started.intent;
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.getAction());
        assertTrue(intent.hasExtra(DocumentsContract.EXTRA_INITIAL_URI));
    }

    private static final class TestProfilesRepository extends BRouterProfilesRepository {
        private final boolean hasTreeGrant;

        private TestProfilesRepository(boolean hasTreeGrant) {
            this.hasTreeGrant = hasTreeGrant;
        }

        @Override
        public boolean hasPersistedProfilesTreeAccess(android.content.Context context) {
            return hasTreeGrant;
        }
    }
}

