package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class BRouterProfilesRepositoryTest {

    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String PRIMARY_ROOT = "primary:";
    private static final String MEDIA_PROFILES_DIR_ID =
            PRIMARY_ROOT + "Android/media/btools.routingapp/brouter/profiles2";

    @Test
    public void getCustomProfilePickerInitialUri_fallsBackToPrimaryMediaProfilesFolderWhenNothingExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
        assertFalse(DocumentsContract.isTreeUri(initialUri));
    }

    @Test
    @Config(sdk = 29)
    public void getCustomProfilePickerInitialUri_stillUsesPrimaryMediaFallbackBeforeAndroid11() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
        assertFalse(DocumentsContract.isTreeUri(initialUri));
    }

    @Test
    public void getCustomProfilePickerInitialUri_usesParentFolderOfSavedCustomProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri customUri = DocumentsContract.buildDocumentUri(
                AUTHORITY,
                MEDIA_PROFILES_DIR_ID + "/trekking.brf"
        );
        repository.saveCustomProfile(activity, customUri, "trekking");

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
        assertFalse(DocumentsContract.isTreeUri(initialUri));
    }

    @Test
    public void saveProfilesTreeUri_roundTripsSavedValue() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, MEDIA_PROFILES_DIR_ID);

        repository.saveProfilesTreeUri(activity, treeUri);

        assertEquals(treeUri, repository.getProfilesTreeUri(activity));
    }

}
