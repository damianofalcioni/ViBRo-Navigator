package vibro.navigator.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidOutputFolderAccessTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private TestOutputDocumentsProvider provider;

    @Before
    public void setUp() throws IOException {
        provider = TestOutputDocumentsProvider.install(context);
        AppOutputFolderSettings.set(context, Kind.LOGS, null);
        AppOutputFolderSettings.set(context, Kind.GPX, null);
    }

    @Test
    public void foldersAreIndependentAndResetToDefault() throws IOException {
        AndroidOutputFolderAccess.accept(context, Kind.LOGS, result());
        assertEquals(TestOutputDocumentsProvider.TREE.toString(), AppOutputFolderSettings.get(context, Kind.LOGS));
        assertNull(AppOutputFolderSettings.get(context, Kind.GPX));
        assertEquals(0, provider.names().length);
        AppOutputFolderSettings.set(context, Kind.LOGS, null);
        assertNull(AndroidOutputFolderAccess.tree(context, Kind.LOGS));
    }

    @Test
    public void missingWriteGrantLeavesPreviousSettingUnchanged() {
        AppOutputFolderSettings.set(context, Kind.GPX, "content://previous/tree/folder");
        Intent readOnly = result().setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertThrows(IOException.class, () -> AndroidOutputFolderAccess.accept(context, Kind.GPX, readOnly));
        assertEquals("content://previous/tree/folder", AppOutputFolderSettings.get(context, Kind.GPX));
    }

    @Test
    public void unavailableFolderLeavesPreviousSettingUnchanged() {
        provider.unavailable = true;
        assertThrows(IOException.class, () -> AndroidOutputFolderAccess.accept(context, Kind.LOGS, result()));
        assertNull(AppOutputFolderSettings.get(context, Kind.LOGS));
    }

    @Test
    public void pickerRequestsPersistentReadWriteAccess() {
        Intent picker = AndroidOutputFolderAccess.picker(context, Kind.GPX);
        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
        assertTrue((picker.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
        assertTrue((picker.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
    }

    private static Intent result() {
        return new Intent().setData(TestOutputDocumentsProvider.TREE)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    }
}
