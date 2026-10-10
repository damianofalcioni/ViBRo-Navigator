package vibro.navigator.android.storage;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exercises ContentResolver streams/publication instead of mocking the output writer. */
public class TestDownloadsProvider extends ContentProvider {
    private final Map<Long, ContentValues> rows = new LinkedHashMap<>();
    private final Map<Long, File> files = new LinkedHashMap<>();
    private long nextId;
    public boolean failWrites;
    public boolean failPublish;
    public boolean failDeletes;

    public static TestDownloadsProvider install(Context context) {
        ProviderInfo info = new ProviderInfo();
        info.authority = "media";
        info.exported = true;
        TestDownloadsProvider provider = Robolectric.buildContentProvider(TestDownloadsProvider.class)
                .create(info).get();
        ShadowContentResolver.registerProviderInternal(info.authority, provider);
        return provider;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = ++nextId;
        ContentValues row = new ContentValues(values);
        row.put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, getContext().getPackageName());
        row.put("volume", uri.getPathSegments().get(0));
        rows.put(id, row);
        files.put(id, new File(getContext().getCacheDir(), "download-" + id));
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public String getType(Uri uri) {
        return rows.get(ContentUris.parseId(uri)).getAsString(MediaStore.MediaColumns.MIME_TYPE);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        if (failPublish) {
            return 0;
        }
        rows.get(ContentUris.parseId(uri)).putAll(values);
        return 1;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        if (failDeletes) {
            return 0;
        }
        long id = ContentUris.parseId(uri);
        rows.remove(id);
        File file = files.remove(id);
        if (file != null) {
            file.delete();
        }
        return 1;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        MatrixCursor cursor = new MatrixCursor(projection);
        for (Map.Entry<Long, ContentValues> entry : rows.entrySet()) {
            if (matches(entry.getValue(), uri, args)) {
                cursor.addRow(values(entry.getKey(), entry.getValue(), projection));
            }
        }
        return cursor;
    }

    private static boolean matches(ContentValues row, Uri uri, String[] args) {
        if (!uri.getPathSegments().get(0).equals(row.getAsString("volume"))) {
            return false;
        }
        return args == null || (args[0].equals(row.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
                && args[1].equals(row.getAsString(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)));
    }

    private static Object[] values(long id, ContentValues row, String[] columns) {
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            values[i] = MediaStore.MediaColumns._ID.equals(columns[i]) ? id : row.get(columns[i]);
        }
        return values;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (failWrites && mode.contains("w")) {
            throw new FileNotFoundException("Downloads storage unavailable");
        }
        return ParcelFileDescriptor.open(files.get(ContentUris.parseId(uri)), ParcelFileDescriptor.parseMode(mode));
    }

    public String read(Uri uri) throws IOException {
        return Files.readString(files.get(ContentUris.parseId(uri)).toPath(), StandardCharsets.UTF_8);
    }

    public ContentValues row(Uri uri) {
        return rows.get(ContentUris.parseId(uri));
    }

    public int count() {
        return rows.size();
    }
}
