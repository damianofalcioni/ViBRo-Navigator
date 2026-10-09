package vibro.navigator.android.storage;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.android.export.AndroidRouteGpxFileProvider;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Prepares folder/file viewing on a worker without changing the selected output folder. */
public final class AndroidOutputFolderBrowser {
    private AndroidOutputFolderBrowser() {
    }

    public static Intent customFolder(Context context, String selected) throws IOException {
        Uri folder = AndroidOutputFolderAccess.ensureFolder(context, Uri.parse(selected));
        Uri document = DocumentsContract.buildDocumentUriUsingTree(folder,
                AndroidWritableDocumentTree.directoryId(folder));
        return view(document, DocumentsContract.Document.MIME_TYPE_DIR)
                .addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    }

    public static List<Entry> defaultFiles(Context context, Kind kind) throws IOException {
        List<Entry> entries = new ArrayList<>();
        addFiles(context, kind, AndroidAppStorageDirs.preferredExternalFilesDir(context), entries);
        addFiles(context, kind, AndroidAppStorageDirs.internalFilesDir(context), entries);
        Collections.sort(entries, (first, second) -> second.name.compareTo(first.name));
        return entries;
    }

    private static void addFiles(Context context, Kind kind, @Nullable File root, List<Entry> entries)
            throws IOException {
        for (File file : folderFiles(root, kind)) {
            if (file.isFile()) {
                Uri uri = AndroidRouteGpxFileProvider.uriForFile(context, file);
                entries.add(new Entry(file.getName(), view(uri, kind == Kind.LOGS
                        ? "text/plain" : NavigationRouteGpxExporter.GPX_MIME_TYPE)));
            }
        }
    }

    private static File[] folderFiles(@Nullable File root, Kind kind) throws IOException {
        if (root == null) {
            return new File[0];
        }
        File folder = new File(root, kind == Kind.LOGS ? "logs" : "gpx");
        if (!folder.exists()) {
            return new File[0];
        }
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IOException("Could not list default output folder");
        }
        return files;
    }

    private static Intent view(Uri uri, String mime) {
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("", uri));
        return intent;
    }

    public static final class Entry {
        public final String name;
        public final Intent intent;

        private Entry(String name, Intent intent) {
            this.name = name;
            this.intent = intent;
        }
    }
}
