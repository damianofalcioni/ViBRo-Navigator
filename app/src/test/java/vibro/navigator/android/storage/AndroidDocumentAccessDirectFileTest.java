package vibro.navigator.android.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import vibro.navigator.brouter.BRouterSegmentReadFile;

@RunWith(RobolectricTestRunner.class)
public class AndroidDocumentAccessDirectFileTest {
    private static final String PRIMARY_ROOT_ID = "primary";
    private static final String PROFILES_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/profiles2";
    private static final String SEGMENTS_DIR_ID =
            "primary:Android/media/btools.routingapp/brouter/segments4";
    private static final String PROFILE_FILE_NAME = "codex-direct-media-profile.brf";
    private static final String SEGMENT_FILE_NAME = "E0_N45.rd5";

    @NonNull
    private final Context context = ApplicationProvider.getApplicationContext();
    @NonNull
    private final List<File> createdFiles = new ArrayList<>();

    @After
    public void deleteCreatedFiles() {
        for (File file : createdFiles) {
            file.delete();
        }
    }

    @Test
    public void externalStorageDocumentExists_acceptsDirectMediaDirectory() throws Exception {
        writeDirectFile(PROFILES_DIR_ID, PROFILE_FILE_NAME, "assign direct = true");

        assertTrue(AndroidDocumentAccess.externalStorageDocumentExists(context, PROFILES_DIR_ID));
    }

    @Test
    public void childAccess_readsDirectMediaProfileFile() throws Exception {
        String profileText = "assign direct = true";
        writeDirectFile(PROFILES_DIR_ID, PROFILE_FILE_NAME, profileText);
        Uri treeUri = AndroidDocumentAccess.buildExternalStorageTreeUri(PROFILES_DIR_ID);

        List<String> names = AndroidDocumentAccess.childDisplayNames(context, treeUri);
        Uri childUri = AndroidDocumentAccess.childDocumentUri(context, treeUri, PROFILE_FILE_NAME);

        assertTrue(names.contains(PROFILE_FILE_NAME));
        assertNotNull(childUri);
        assertEquals("file", childUri.getScheme());
        assertEquals(profileText, AndroidDocumentAccess.readText(context, childUri));
    }

    @Test
    public void documentReadFile_opensDirectMediaFileUri() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        File file = writeDirectFile(SEGMENTS_DIR_ID, SEGMENT_FILE_NAME, bytes);
        byte[] out = new byte[3];

        try (BRouterSegmentReadFile readFile = AndroidDocumentReadFile.open(context, Uri.fromFile(file))) {
            assertNotNull(readFile);
            assertEquals(bytes.length, readFile.length());
            readFile.readFully(1L, out, 0, out.length);
        }

        assertArrayEquals(new byte[]{2, 3, 4}, out);
    }

    @NonNull
    private File writeDirectFile(
            @NonNull String directoryDocumentId,
            @NonNull String fileName,
            @NonNull String text
    ) throws IOException {
        return writeDirectFile(directoryDocumentId, fileName, text.getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    private File writeDirectFile(
            @NonNull String directoryDocumentId,
            @NonNull String fileName,
            @NonNull byte[] bytes
    ) throws IOException {
        File directory = directDirectory(directoryDocumentId);
        assertTrue(directory.mkdirs() || directory.isDirectory());
        File file = new File(directory, fileName);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
        createdFiles.add(file);
        return file;
    }

    @NonNull
    private File directDirectory(@NonNull String documentId) {
        String relativePath = documentId.substring(documentId.indexOf(':') + 1);
        File root = AndroidStorageVolumes.storageRoot(context, PRIMARY_ROOT_ID);
        assertNotNull(root);
        return new File(root, relativePath);
    }
}
