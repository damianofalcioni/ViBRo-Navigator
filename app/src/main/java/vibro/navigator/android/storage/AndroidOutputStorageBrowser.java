package vibro.navigator.android.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

/** Opens the selected folder in a browser task separate from ViBRo's main task. */
public final class AndroidOutputStorageBrowser {
    private AndroidOutputStorageBrowser() { }

    public static void prepare(Context context, AndroidOutputDestination destination) throws IOException {
        if (destination.directory != null) {
            destination.ensureDirectory();
        } else if (destination.tree != null) {
            destination.ensureTree(context);
        } else {
            AndroidOutputStorage.verify(context, destination);
        }
    }

    public static void open(Activity activity, AndroidOutputDestination destination) {
        // The separate host keeps picker results out of ViBRo's main task.
        activity.startActivity(new Intent(activity, AndroidOutputBrowserActivity.class)
                .setData(folderUri(activity, destination))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private static Uri folderUri(Context context, AndroidOutputDestination destination) {
        if (destination.tree != null) {
            return AndroidOutputFolderRecovery.accessUri(context, destination.tree);
        }
        String path = destination.directory == null ? destination.label : destination.directory.getAbsolutePath();
        boolean primary = destination.directory != null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(destination.volume));
        return documentForPath(path.replace(File.separatorChar, '/'), primary);
    }

    private static Uri documentForPath(String path, boolean primary) {
        int android = path.indexOf("/Android/");
        int download = path.indexOf("/Download/");
        int start = android >= 0 ? android : download;
        if (start < 0) {
            return DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary");
        }
        String root = new File(path.substring(0, start)).getName();
        String id = primary ? "primary" : root;
        return AndroidDocumentAccess.buildExternalStorageDocumentUri(id + ":" + path.substring(start + 1));
    }
}
