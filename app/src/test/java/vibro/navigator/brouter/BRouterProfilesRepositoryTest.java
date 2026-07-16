package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class BRouterProfilesRepositoryTest {

    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String PRIMARY_ROOT = "primary:";
    private static final String MEDIA_PROFILES_DIR_ID =
            PRIMARY_ROOT + "Android/media/btools.routingapp/brouter/profiles2";
    private static final String PROFILE_TREKKING = "trekking";
    private static final String PROFILE_FASTBIKE = "fastbike";
    private static final String PARAM_AVOID_PATH = "avoid_path";
    private static final String PARAM_UPHILL_COST = "uphillcost";

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
                MEDIA_PROFILES_DIR_ID + "/" + PROFILE_TREKKING + ".brf"
        );
        repository.saveCustomProfile(activity, customUri, PROFILE_TREKKING);

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

    @Test
    public void saveProfileParameterValues_storesOnlyChangedValuesByProfileName() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        List<BRouterProfileParameter> parameters = parameters();
        Map<String, String> values = new HashMap<>();
        values.put(PARAM_AVOID_PATH, "1");
        values.put(PARAM_UPHILL_COST, "80");

        repository.saveProfileParameterValues(activity, PROFILE_TREKKING, parameters, values);

        assertEquals(PARAM_AVOID_PATH + "=1",
                repository.getProfileParameterOverridesExtraParams(activity, PROFILE_TREKKING));
        assertEquals(0, repository.getProfileParameterValueOverrides(activity, PROFILE_FASTBIKE).size());
    }

    @Test
    public void resetProfileParameterValues_clearsSavedOverridesForProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = AndroidBRouterProfilesRepositoryFactory.create();
        Map<String, String> values = new HashMap<>();
        values.put(PARAM_AVOID_PATH, "1");
        repository.saveProfileParameterValues(activity, PROFILE_TREKKING, parameters(), values);

        repository.resetProfileParameterValues(activity, PROFILE_TREKKING);

        assertEquals(0, repository.getProfileParameterValueOverrides(activity, PROFILE_TREKKING).size());
    }

    @Test
    public void getProfileParameters_readsSavedCustomProfileTextByMatchingProfileName() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri customUri = Uri.parse("content://profiles/" + PROFILE_TREKKING + ".brf");
        BRouterProfilesRepository repository = new BRouterProfilesRepository(
                TestProfileParameterDependencies.create(customUri, "assign " + PARAM_AVOID_PATH + " = false"
                        + " # %" + PARAM_AVOID_PATH + "% | Avoid paths | boolean")
        );
        repository.saveCustomProfile(activity, customUri, PROFILE_TREKKING);

        List<BRouterProfileParameter> parameters = repository.getProfileParameters(activity, PROFILE_TREKKING);

        assertEquals(1, parameters.size());
        assertEquals(PARAM_AVOID_PATH, parameters.get(0).name);
    }

    @Test
    public void getCustomProfileText_readsOnlyMatchingSavedCustomProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Uri customUri = Uri.parse("content://profiles/" + PROFILE_TREKKING + ".brf");
        String profileText = "assign " + PARAM_AVOID_PATH + " = false";
        BRouterProfilesRepository repository = new BRouterProfilesRepository(
                TestProfileParameterDependencies.create(customUri, profileText)
        );
        repository.saveCustomProfile(activity, customUri, PROFILE_TREKKING);

        assertEquals(profileText, repository.getCustomProfileText(activity, PROFILE_TREKKING));
        assertNull(repository.getCustomProfileText(activity, PROFILE_FASTBIKE));
    }

    @NonNull
    private static List<BRouterProfileParameter> parameters() {
        return Arrays.asList(
                new BRouterProfileParameter(
                        PARAM_AVOID_PATH,
                        "Avoid paths",
                        "0",
                        BRouterProfileParameter.ValueType.BOOLEAN,
                        null
                ),
                new BRouterProfileParameter(
                        PARAM_UPHILL_COST,
                        "Uphill cost",
                        "80",
                        BRouterProfileParameter.ValueType.NUMBER,
                        null
                )
        );
    }

}
