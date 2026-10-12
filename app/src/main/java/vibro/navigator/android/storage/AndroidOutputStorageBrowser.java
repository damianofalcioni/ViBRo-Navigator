package vibro.navigator.android.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.IntentCompat;

import java.io.File;
import java.io.IOException;

/** Opens the selected folder in a browser task separate from ViBRo's main task. */
public final class AndroidOutputStorageBrowser {
    private static final String EXTRA_PICKER = "vibro.navigator.storage.PICKER";
    private static final String EXTRA_RETURN_TO = "vibro.navigator.storage.RETURN_TO";
    private static final String EXTRA_REQUEST_CODE = "vibro.navigator.storage.REQUEST_CODE";
    private static final String EXTRA_RESULT_CODE = "vibro.navigator.storage.RESULT_CODE";

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
        activity.startActivity(hostIntent(activity).setData(folderUri(activity, destination)));
    }

    public static void openForResult(Activity activity, Intent picker, int requestCode) {
        Intent returnTo = new Intent(activity, activity.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_REQUEST_CODE, requestCode);
        activity.startActivity(hostIntent(activity).putExtra(EXTRA_PICKER, picker).putExtra(EXTRA_RETURN_TO, returnTo));
    }

    private static Intent hostIntent(Activity activity) {
        return new Intent(activity, AndroidOutputBrowserActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    static Intent pickerIntent(Intent launch) {
        Intent picker = IntentCompat.getParcelableExtra(launch, EXTRA_PICKER, Intent.class);
        return picker == null ? AndroidDocumentAccess.openDocumentIntent(launch.getData()) : picker;
    }

    static boolean returnResult(Activity host, int resultCode, Intent data) {
        Intent returnTo = IntentCompat.getParcelableExtra(host.getIntent(), EXTRA_RETURN_TO, Intent.class);
        if (returnTo == null) {
            return false;
        }
        Intent result = new Intent(returnTo).putExtra(EXTRA_RESULT_CODE, resultCode);
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Starting the caller with the selected URI transfers read access to its activity
            // before this host finishes and releases the system picker's temporary grant.
            result.setData(data.getData()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            result.addFlags(data.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            result.setClipData(data.getClipData());
        }
        host.startActivity(result);
        return true;
    }

    public static boolean hasResult(Intent intent) {
        return intent.hasExtra(EXTRA_RESULT_CODE);
    }

    public static void dispatchResult(Intent intent, ActivityResult receiver) {
        if (!hasResult(intent)) {
            return;
        }
        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1);
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        // Consume before dispatch so recreation cannot apply the same document twice.
        intent.removeExtra(EXTRA_REQUEST_CODE);
        intent.removeExtra(EXTRA_RESULT_CODE);
        receiver.onActivityResult(requestCode, resultCode, intent);
    }

    public interface ActivityResult {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    public static Uri folderUri(Context context, AndroidOutputDestination destination) {
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
