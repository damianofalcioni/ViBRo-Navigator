package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.logging.AppLogger;

final class AndroidBRouterTreePicker {
    private AndroidBRouterTreePicker() {
    }

    static void launch(Activity activity, @Nullable Uri initialUri, int requestCode,
                       String logTag, Runnable onLaunched, Runnable onFailure) {
        try {
            activity.startActivityForResult(AndroidDocumentAccess.openDocumentTreeIntent(initialUri), requestCode);
        } catch (ActivityNotFoundException | SecurityException e) {
            AppLogger.w(logTag, "Unable to open BRouter folder picker", e);
            Toast.makeText(activity, R.string.msg_output_folder_picker_unavailable, Toast.LENGTH_SHORT).show();
            onFailure.run();
            return;
        }
        AppLogger.i(logTag, "Opened BRouter folder picker initialUri=" + initialUri);
        onLaunched.run();
    }
}
