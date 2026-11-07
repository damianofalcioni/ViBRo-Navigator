package vibro.navigator.android.storage;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.attribute.BasicFileAttributes;

import vibro.navigator.android.storage.AndroidDocumentAccess.DirectoryPresence;

final class AndroidExternalDirectoryPresence {
    private AndroidExternalDirectoryPresence() {
    }

    @NonNull
    static DirectoryPresence check(@NonNull Context context, @NonNull String documentId) {
        File directory = AndroidExternalStorageDocumentFiles.fileForDocumentId(context, documentId);
        if (directory == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return DirectoryPresence.UNKNOWN;
        }
        return statDirectory(directory);
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.O)
    private static DirectoryPresence statDirectory(@NonNull File directory) {
        try {
            return Files.readAttributes(directory.toPath(), BasicFileAttributes.class).isDirectory()
                    ? DirectoryPresence.PRESENT : DirectoryPresence.MISSING;
        } catch (NoSuchFileException | NotDirectoryException e) {
            return DirectoryPresence.MISSING;
        } catch (IOException | SecurityException e) {
            return DirectoryPresence.UNKNOWN;
        }
    }
}
