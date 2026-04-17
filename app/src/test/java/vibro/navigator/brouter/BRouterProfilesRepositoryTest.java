package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class BRouterProfilesRepositoryTest {

    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String MEDIA_PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";
    private static final String LEGACY_PROFILES_DIR_ID =
            "primary:Android/data/btools.routingapp/files/brouter/profiles2";

    @Test
    public void getCustomProfilePickerInitialUri_fallsBackToPrimaryMediaProfilesFolderWhenNothingExists() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
        assertFalse(DocumentsContract.isTreeUri(initialUri));
    }

    @Test
    @Config(sdk = 29)
    public void getCustomProfilePickerInitialUri_stillUsesPrimaryMediaFallbackBeforeAndroid11() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();

        Uri initialUri = repository.getCustomProfilePickerInitialUri(activity);

        assertEquals(MEDIA_PROFILES_DIR_ID, DocumentsContract.getDocumentId(initialUri));
        assertFalse(DocumentsContract.isTreeUri(initialUri));
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
        assertFalse(DocumentsContract.isTreeUri(initialUri));
    }

    @Test
    public void saveProfilesTreeUri_roundTripsSavedValue() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        BRouterProfilesRepository repository = new BRouterProfilesRepository();
        Uri treeUri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, MEDIA_PROFILES_DIR_ID);

        repository.saveProfilesTreeUri(activity, treeUri);

        assertEquals(treeUri, repository.getProfilesTreeUri(activity));
    }

    @Test
    public void buildLikelyPickerDocumentIdCandidates_checksMediaAndDataForAllRootsWithoutSdkSwitch() {
        List<String> candidates = BRouterProfilesRepository.buildLikelyPickerDocumentIdCandidates(
                Collections.singletonList("B4DD-C8AC")
        );

        assertEquals("B4DD-C8AC:" + MEDIA_PROFILES_DIR_ID.substring("primary:".length()), candidates.get(0));
        assertEquals(MEDIA_PROFILES_DIR_ID, candidates.get(1));
        assertEquals("B4DD-C8AC:" + LEGACY_PROFILES_DIR_ID.substring("primary:".length()), candidates.get(2));
        assertEquals(LEGACY_PROFILES_DIR_ID, candidates.get(3));
    }

    @Test
    public void buildFallbackPickerDocumentIdCandidates_prefersPrimaryBeforeSecondaryRoots() {
        List<String> candidates = BRouterProfilesRepository.buildFallbackPickerDocumentIdCandidates(
                Collections.singletonList("0000-0000")
        );

        assertEquals(MEDIA_PROFILES_DIR_ID, candidates.get(0));
        assertEquals(LEGACY_PROFILES_DIR_ID, candidates.get(1));
        assertEquals("0000-0000:" + MEDIA_PROFILES_DIR_ID.substring("primary:".length()), candidates.get(2));
        assertEquals("0000-0000:" + LEGACY_PROFILES_DIR_ID.substring("primary:".length()), candidates.get(3));
    }

    @Test
    public void listProfiles_mergesDiscoveredExternalTreesWithBundledProfiles() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TestRepository repository = new TestRepository();
        Uri internalTree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, MEDIA_PROFILES_DIR_ID);
        Uri sdTree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "B4DD-C8AC:" + MEDIA_PROFILES_DIR_ID.substring("primary:".length()));
        repository.discoveryTreeUris = Arrays.asList(internalTree, sdTree);
        repository.treeProfiles.put(internalTree.toString(), Arrays.asList("trekking", "fastbike"));
        repository.treeProfiles.put(sdTree.toString(), Arrays.asList("gravel", "trekking"));
        repository.bundledProfiles = Arrays.asList("car-eco", "fastbike");

        List<String> profiles = repository.listProfiles(activity);

        assertEquals(Arrays.asList("car-eco", "fastbike", "gravel", "trekking"), profiles);
    }

    static final class TestRepository extends BRouterProfilesRepository {
        private List<Uri> discoveryTreeUris = Collections.emptyList();
        private final Map<String, List<String>> treeProfiles = new HashMap<>();
        private List<String> bundledProfiles = Collections.emptyList();

        @Override
        List<Uri> resolveProfilesDiscoveryTreeUris(Context context) {
            return discoveryTreeUris;
        }

        @Override
        List<String> listProfilesFromTree(Context context, Uri treeUri) {
            List<String> profiles = treeProfiles.get(treeUri.toString());
            return profiles != null ? profiles : Collections.emptyList();
        }

        @Override
        List<String> listBundledProfiles(Context context) {
            return bundledProfiles;
        }
    }
}
