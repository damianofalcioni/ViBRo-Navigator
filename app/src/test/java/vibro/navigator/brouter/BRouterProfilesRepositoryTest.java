package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class BRouterProfilesRepositoryTest {

    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String MEDIA_PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_PROFILES_DIR_ID =
            "primary:Android/data/btools.routingapp/files/brouter/profiles2";

    @Test
    public void getCustomProfilePickerInitialUri_defaultsToProfilesFolderDocument() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
    }

    @Test
    @Config(sdk = 29)
    public void getCustomProfilePickerInitialUri_defaultsToLegacyProfilesFolderBeforeAndroid11() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(LEGACY_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
    }

    @Test
    public void getCustomProfilePickerInitialUri_usesSavedProfilesTreeFolder() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, MEDIA_PROFILES_DIR_ID);
        repository.saveProfilesTreeUri(activity, treeUri);

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
    }

    @Test
    public void getCustomProfilePickerInitialUri_usesParentFolderOfSavedCustomProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();
        Uri customUri = DocumentsContract.buildDocumentUri(
                AUTHORITY,
                MEDIA_PROFILES_DIR_ID + "/trekking.brf"
        );
        repository.saveCustomProfile(activity, customUri, "trekking");

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
    }
}
