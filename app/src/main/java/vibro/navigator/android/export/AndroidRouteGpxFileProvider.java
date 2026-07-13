package vibro.navigator.android.export;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import vibro.navigator.nav.export.NavigationRouteGpxExporter;

public final class AndroidRouteGpxFileProvider extends ContentProvider {
    private static final String FILE_PROVIDER_SUFFIX = ".fileprovider";
    private static final String READ_MODE = "r";

    @NonNull
    static Uri uriForFile(@NonNull Context context, @NonNull File file) throws IOException {
        return AndroidRouteGpxProviderUri.uriForFile(context, file, authority(context));
    }

    @NonNull
    private static String authority(@NonNull Context context) {
        return context.getPackageName() + FILE_PROVIDER_SUFFIX;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    @Nullable
    public String getType(@NonNull Uri uri) {
        return NavigationRouteGpxExporter.GPX_MIME_TYPE;
    }

    @Override
    @Nullable
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder
    ) {
        try {
            return AndroidRouteGpxProviderMetadata.cursor(fileForUri(uri), projection);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    @Nullable
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs
    ) {
        return 0;
    }

    @Override
    @NonNull
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (!READ_MODE.equals(mode)) {
            throw new FileNotFoundException("Route GPX exports are read-only");
        }
        return ParcelFileDescriptor.open(fileForUri(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @NonNull
    private File fileForUri(@NonNull Uri uri) throws FileNotFoundException {
        return AndroidRouteGpxProviderUri.fileForUri(providerContext(), uri);
    }

    @NonNull
    private Context providerContext() throws FileNotFoundException {
        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Route GPX provider is not attached");
        }
        return context;
    }
}
