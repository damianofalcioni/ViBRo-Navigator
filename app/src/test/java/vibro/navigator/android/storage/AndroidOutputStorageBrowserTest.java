package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.io.IOException;

import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 28}, shadows = TestOutputMediaDirs.class)
public class AndroidOutputStorageBrowserTest {
    @Test
    public void preparesEachSdSubfolderAndOpensStandaloneBrowserWithoutChangingTheGrant() throws IOException {
        Application app = ApplicationProvider.getApplicationContext();
        TestOutputStorageCard card = new TestOutputStorageCard(app);
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(card.context,
                "com.android.externalstorage.documents", TestOutputStorageCard.ID + ":");
        Uri grant = provider.tree(TestOutputStorageCard.ID + ":");
        AndroidOutputStorageAccess.accept(card.context, new Intent().setData(grant).addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            for (Kind kind : Kind.values()) {
                AndroidOutputDestination destination = AndroidOutputStorage.requested(card.context, kind);
                AndroidOutputStorageBrowser.prepare(card.context, destination);
                AndroidOutputStorageBrowser.open(controller.get(), destination);
                Intent viewed = shadowOf(controller.get()).getNextStartedActivityForResult().intent;
                assertEquals(AndroidOutputBrowserActivity.class.getName(), viewed.getComponent().getClassName());
                assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, viewed.getFlags());
                assertEquals(DocumentsContract.getTreeDocumentId(grant),
                        DocumentsContract.getTreeDocumentId(viewed.getData()));
                assertEquals(TestOutputStorageCard.ID + ":Download/ViBRo/" + AndroidOutputDestination.subfolder(kind),
                        DocumentsContract.getDocumentId(viewed.getData()));
                assertNull(viewed.getClipData());
                assertTrue(AndroidOutputFolderRecovery.hasGrant(card.context, destination.tree));
                assertEquals(destination.token(), AndroidOutputStorage.requested(card.context, kind).token());
            }
        }
    }
}
