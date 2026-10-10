package vibro.navigator.android.storage;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import vibro.navigator.settings.AppOutputFolderSettings.Kind;

final class AndroidOutputStorageCleaner {
    private AndroidOutputStorageCleaner() { }

    static void clear(Context context, Kind kind, String token) throws IOException {
        AndroidOutputDestination destination = AndroidOutputDestination.fromToken(token);
        if (destination.tree != null) {
            AndroidOutputFolderCleaner.clear(context, kind, destination.tree.toString());
        } else if (destination.directory != null) {
            clearFiles(destination.directory, kind);
        } else {
            clearDownloads(context, destination, kind);
        }
    }

    private static Pattern names(Kind kind) {
        String prefix = kind == Kind.LOGS ? "log" : "route";
        String suffix = kind == Kind.LOGS ? "txt" : "gpx";
        // MediaStore may add its own " (1)" collision suffix to a requested display name.
        return Pattern.compile("vibro-navigator-" + prefix + "-\\d{14}(?:-\\d+)?(?: \\(\\d+\\))?\\." + suffix);
    }

    private static void clearFiles(File directory, Kind kind) throws IOException {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Could not list output files");
        }
        Pattern pattern = names(kind);
        for (File file : files) {
            deleteFile(file, pattern);
        }
    }

    private static void deleteFile(File file, Pattern names) throws IOException {
        if (file.isFile() && names.matcher(file.getName()).matches() && !file.delete()) {
            throw new IOException("Could not delete output file");
        }
    }

    private static void clearDownloads(Context context, AndroidOutputDestination destination, Kind kind)
            throws IOException {
        for (long id : downloadIds(context, destination, names(kind))) {
            if (context.getContentResolver().delete(ContentUris.withAppendedId(destination.collection(), id),
                    null, null) != 1) {
                throw new IOException("Could not delete output download");
            }
        }
    }

    private static List<Long> downloadIds(Context context, AndroidOutputDestination destination, Pattern names)
            throws IOException {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(destination.collection(), new String[]{
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME},
                MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.OWNER_PACKAGE_NAME + "=?",
                new String[]{destination.relativePath, context.getPackageName()}, null)) {
            if (cursor == null) {
                throw new IOException("Could not list output downloads");
            }
            while (cursor.moveToNext()) {
                if (names.matcher(cursor.getString(1)).matches()) {
                    ids.add(cursor.getLong(0));
                }
            }
        }
        return ids;
    }
}
