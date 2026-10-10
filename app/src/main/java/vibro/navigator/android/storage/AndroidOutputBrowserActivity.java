package vibro.navigator.android.storage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import vibro.navigator.R;

/** Hosts system file picking in its own task without creating an in-app file browser. */
public final class AndroidOutputBrowserActivity extends Activity {
    static final int REQUEST_OPEN_FILE = 4013;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            try {
                startActivityForResult(AndroidDocumentAccess.openDocumentIntent(getIntent().getData()),
                        REQUEST_OPEN_FILE);
            } catch (ActivityNotFoundException | SecurityException e) {
                unavailable();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_FILE) {
            return;
        }
        try {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(data.getData(),
                        getContentResolver().getType(data.getData()))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            unavailable();
        } finally {
            finish();
        }
    }

    private void unavailable() {
        Toast.makeText(this, R.string.msg_output_viewer_unavailable, Toast.LENGTH_LONG).show();
    }
}
