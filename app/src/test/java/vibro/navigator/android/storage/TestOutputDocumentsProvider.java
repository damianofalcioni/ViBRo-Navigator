package vibro.navigator.android.storage;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Local SAF provider with real file descriptors, directory metadata and revocable availability. */
public class TestOutputDocumentsProvider extends DocumentsProvider {
    private static final String AUTHORITY = "vibro.test.documents";
    public static final Uri TREE = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "output");
    private static final String[] COLUMNS = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS};
    private final Map<String, File> documents = new LinkedHashMap<>();
    private String authority;
    private File directory;
    public boolean unavailable;
    public boolean failWrites;
    public boolean failDeletes;

    public static TestOutputDocumentsProvider install(Context context) throws IOException {
        return install(context, AUTHORITY, "output");
    }

    public static TestOutputDocumentsProvider install(Context context, String authority, String rootId) throws IOException {
        ProviderInfo info = new ProviderInfo();
        info.authority = authority;
        info.exported = true;
        info.grantUriPermissions = true;
        info.readPermission = "android.permission.MANAGE_DOCUMENTS";
        info.writePermission = info.readPermission;
        TestOutputDocumentsProvider provider = Robolectric.buildContentProvider(TestOutputDocumentsProvider.class)
                .create(info).get();
        provider.authority = authority;
        provider.directory = Files.createTempDirectory(context.getCacheDir().toPath(), "output-test-").toFile();
        provider.documents.put(rootId, provider.directory);
        ShadowContentResolver.registerProviderInternal(authority, provider);
        context.getContentResolver().takePersistableUriPermission(provider.tree(rootId),
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        AndroidOutputFolderAccess.markAvailable(Kind.LOGS);
        AndroidOutputFolderAccess.markAvailable(Kind.GPX);
        return provider;
    }

    public Uri tree(String id) {
        return DocumentsContract.buildTreeDocumentUri(authority, id);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        return new MatrixCursor(new String[]{DocumentsContract.Root.COLUMN_ROOT_ID});
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = cursor(projection);
        cursor.addRow(DocumentRow.values(cursor.getColumnNames(), documentId, file(documentId)));
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        File parent = file(parentDocumentId);
        MatrixCursor cursor = cursor(projection);
        for (Map.Entry<String, File> entry : documents.entrySet()) {
            if (parent.equals(entry.getValue().getParentFile())) {
                cursor.addRow(DocumentRow.values(cursor.getColumnNames(), entry.getKey(), entry.getValue()));
            }
        }
        return cursor;
    }

    private static MatrixCursor cursor(String[] projection) {
        return new MatrixCursor(projection == null ? COLUMNS : projection);
    }

    private static final class DocumentRow {
        static Object[] values(String[] columns, String id, File file) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                row[i] = column(columns[i], id, file);
            }
            return row;
        }

        private static Object column(String column, String id, File file) {
            return switch (column) {
                case DocumentsContract.Document.COLUMN_DOCUMENT_ID -> id;
                case DocumentsContract.Document.COLUMN_DISPLAY_NAME -> file.getName();
                case DocumentsContract.Document.COLUMN_MIME_TYPE -> file.isDirectory()
                        ? DocumentsContract.Document.MIME_TYPE_DIR : "text/plain";
                case DocumentsContract.Document.COLUMN_FLAGS -> file.isDirectory()
                        ? DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE : DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
                default -> null;
            };
        }
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        File child = new File(file(parentDocumentId), displayName);
        try {
            boolean created = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)
                    ? child.mkdir() : child.createNewFile();
            if (!created) {
                throw new IOException("Could not create test document");
            }
        } catch (IOException e) {
            FileNotFoundException error = new FileNotFoundException(e.getMessage());
            error.initCause(e);
            throw error;
        }
        String id = parentDocumentId + (parentDocumentId.endsWith(":") ? "" : "/") + displayName;
        documents.put(id, child);
        return id;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        if (failWrites && mode.contains("w")) {
            throw new FileNotFoundException("Provider rejects writes");
        }
        return ParcelFileDescriptor.open(file(documentId), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        if (failDeletes) {
            throw new FileNotFoundException("Provider rejects deletion");
        }
        if (!file(documentId).delete()) {
            throw new FileNotFoundException("Could not delete test document");
        }
        documents.remove(documentId);
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        String prefix = parentDocumentId.endsWith(":") ? parentDocumentId : parentDocumentId + "/";
        return documentId.equals(parentDocumentId) || documentId.startsWith(prefix);
    }

    public String[] names() {
        String[] result = directory.list();
        return result == null ? new String[0] : result;
    }

    public String read(String nameOrId) throws IOException {
        File file = documents.get(nameOrId);
        return new String(Files.readAllBytes((file == null ? new File(directory, nameOrId) : file).toPath()),
                StandardCharsets.UTF_8);
    }

    private File file(String id) throws FileNotFoundException {
        File file = documents.get(id);
        if (unavailable || file == null || !file.exists()) {
            throw new FileNotFoundException("Folder or document is unavailable");
        }
        return file;
    }
}
