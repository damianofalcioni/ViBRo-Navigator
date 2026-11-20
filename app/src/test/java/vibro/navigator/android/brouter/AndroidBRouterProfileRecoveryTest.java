package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterProfileRecoveryTest {
    private static final String REPLACEMENT = "replacement-profile-dir";

    @Test
    public void obsoleteEmptyChildListingIsIncompleteUntilTheReplacementIsRead() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Uri tree = DocumentsContract.buildTreeDocumentUri("test.brouter.documents", "root");
            var provider = new AndroidBRouterParentGrantTest.ParentProvider();
            ShadowContentResolver.registerProviderInternal(tree.getAuthority(), provider);
            assertTrue(TestBRouterStorage.saveSelection(activity, new Intent().setData(tree)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)));
            provider.opaqueProfiles = REPLACEMENT;

            var results = new ArrayList<AndroidBRouterProfileLoader.Result>();
            var repository = AndroidBRouterProfilesRepositoryFactory.create();
            var loader = new AndroidBRouterProfileLoader(activity, repository, results::add);
            loader.refresh();

            var recovering = results.get(results.size() - 1);
            assertFalse(recovering.complete());
            assertTrue(recovering.profiles().isEmpty());

            loader.refresh();

            var recovered = results.get(results.size() - 1);
            assertTrue(recovered.complete());
            assertEquals(List.of("shared-parent-test"), recovered.profiles());
            loader.dispose();
        }
    }
}
