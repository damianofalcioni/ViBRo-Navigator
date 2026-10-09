package vibro.navigator.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import vibro.navigator.android.export.AndroidRouteGpxFileProvider;
import vibro.navigator.android.storage.AndroidOutputFolderBrowser.Entry;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidOutputFolderBrowserTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void customFolderViewsDirectoryWithReadGrantWithoutChangingPreference() throws IOException {
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(context);
        String selected = TestOutputDocumentsProvider.TREE.toString();
        AppOutputFolderSettings.set(context, Kind.LOGS, selected);
        Intent intent = AndroidOutputFolderBrowser.customFolder(context, selected);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, intent.getType());
        assertEquals(DocumentsContract.buildDocumentUriUsingTree(TestOutputDocumentsProvider.TREE, "output"),
                intent.getData());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertEquals(0, intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertNotNull(intent.getClipData());
        assertEquals(selected, AppOutputFolderSettings.get(context, Kind.LOGS));
        assertEquals(0, provider.names().length);
    }

    @Test
    public void revokedCustomFolderDoesNotRedirectToDefault() throws IOException {
        TestOutputDocumentsProvider.install(context);
        String selected = TestOutputDocumentsProvider.TREE.toString();
        AppOutputFolderSettings.set(context, Kind.GPX, selected);
        context.getContentResolver().releasePersistableUriPermission(TestOutputDocumentsProvider.TREE,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertThrows(IOException.class, () -> AndroidOutputFolderBrowser.customFolder(context, selected));
        assertEquals(selected, AppOutputFolderSettings.get(context, Kind.GPX));
    }

    @Test
    public void defaultsExposeBothStorageLocationsReadOnlyAndExcludeSubfolders() throws Exception {
        File external = new File(AndroidAppStorageDirs.preferredExternalFilesDir(context), "logs");
        File internal = new File(context.getFilesDir(), "logs");
        assertTrue(external.isDirectory() || external.mkdirs());
        assertTrue(internal.isDirectory() || internal.mkdirs());
        File first = new File(external, "browser-external.txt");
        File second = new File(internal, "browser-internal.log");
        Files.writeString(first.toPath(), "external details");
        Files.writeString(second.toPath(), "internal details");
        File child = new File(internal, "browser-subfolder");
        assertTrue(child.mkdir());
        try {
            List<Entry> files = AndroidOutputFolderBrowser.defaultFiles(context, Kind.LOGS);
            Entry externalEntry = entry(files, first.getName());
            Entry internalEntry = entry(files, second.getName());
            assertEquals("external details", read(externalEntry.intent.getData()));
            assertEquals("internal details", read(internalEntry.intent.getData()));
            assertEquals("text/plain", context.getContentResolver().getType(internalEntry.intent.getData()));
            assertTrue(files.stream().noneMatch(file -> file.name.equals(child.getName())));
            assertThrows(FileNotFoundException.class, () -> context.getContentResolver()
                    .openFileDescriptor(externalEntry.intent.getData(), "w"));
        } finally {
            Files.delete(first.toPath());
            Files.delete(second.toPath());
            Files.delete(child.toPath());
        }
    }

    @Test
    public void fileProviderRejectsEscapingAppStorage() {
        File outside = new File(context.getFilesDir(), "../private-file.txt");
        assertThrows(IOException.class, () -> AndroidRouteGpxFileProvider.uriForFile(context, outside));
        Uri escaped = Uri.parse("content://" + context.getPackageName() + ".fileprovider/internal/../private-file.txt");
        assertThrows(FileNotFoundException.class, () -> context.getContentResolver().openInputStream(escaped));
    }

    private static Entry entry(List<Entry> files, String name) {
        return files.stream().filter(file -> file.name.equals(name)).findFirst().orElseThrow(AssertionError::new);
    }

    private String read(Uri uri) throws IOException {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
