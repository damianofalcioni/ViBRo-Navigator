package vibro.navigator.android.storage;

import android.app.Activity;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

/** Leaves the simulated permission dialog pending until the test supplies the user's answer. */
@Implements(Activity.class)
public class TestOutputPermissionActivity extends ShadowActivity {
    private PermissionsRequest pending;

    @Implementation
    protected void requestPermissions(String[] permissions, int code) {
        pending = new PermissionsRequest(permissions, code);
    }

    @Override
    public PermissionsRequest getLastRequestedPermission() {
        return pending;
    }
}
