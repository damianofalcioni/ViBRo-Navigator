package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class BRouterProfileListerTest {

    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private static final String MEDIA_PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";

    @Test
    public void listProfiles_mergesDiscoveredExternalTreesWithBundledProfiles() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TestProfileLister lister = new TestProfileLister();
        Uri internalTree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, MEDIA_PROFILES_DIR_ID);
        Uri sdTree = DocumentsContract.buildTreeDocumentUri(
                AUTHORITY,
                "B4DD-C8AC:" + MEDIA_PROFILES_DIR_ID.substring("primary:".length())
        );
        lister.treeProfiles.put(internalTree.toString(), Arrays.asList("trekking", "fastbike"));
        lister.treeProfiles.put(sdTree.toString(), Arrays.asList("gravel", "trekking"));
        lister.bundledProfiles = Arrays.asList("car-eco", "fastbike");

        List<String> profiles = lister.listProfiles(activity, Arrays.asList(internalTree, sdTree));

        assertEquals(Arrays.asList("car-eco", "fastbike", "gravel", "trekking"), profiles);
    }

    static final class TestProfileLister extends BRouterProfileLister {
        private final Map<String, List<String>> treeProfiles = new HashMap<>();
        private List<String> bundledProfiles = Collections.emptyList();

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
