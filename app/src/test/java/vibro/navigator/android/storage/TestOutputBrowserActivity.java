package vibro.navigator.android.storage;

import android.app.Activity;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

/** Records task removal, which Robolectric's default activity shadow treats as finish(). */
@Implements(Activity.class)
public class TestOutputBrowserActivity extends ShadowActivity {
    boolean taskRemoved;

    @Override
    @Implementation
    protected void finishAndRemoveTask() {
        taskRemoved = true;
        super.finishAndRemoveTask();
    }
}
