package vibro.navigator.main;

import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class MainActivityIncomingLocationLifecycleTest {
    private static final String DESTINATION = "Existing destination";
    private static final String SHARED_ADDRESS = "Cafe Central";

    @Test
    public void newIntentPreservesDestinationAndRecreationDoesNotDuplicateStop() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.destinationEdit)).setText(DESTINATION);

            controller.newIntent(sharedAddress());
            assertRoute(activity);
            controller.recreate();

            assertRoute(controller.get());
        }
    }

    @Test
    public void pendingIncomingLocationIsAppliedAfterFormRestoration() {
        Bundle saved = new Bundle();
        try (ActivityController<MainActivity> original = Robolectric.buildActivity(MainActivity.class).setup()) {
            ((EditText) original.get().findViewById(R.id.destinationEdit)).setText(DESTINATION);
            original.saveInstanceState(saved);
        }

        try (ActivityController<MainActivity> restored = Robolectric.buildActivity(MainActivity.class, sharedAddress())) {
            restored.create(saved).start().restoreInstanceState(saved).postCreate(saved).resume();

            assertRoute(restored.get());
        }
    }

    private static Intent sharedAddress() {
        return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, SHARED_ADDRESS);
    }

    private static void assertRoute(MainActivity activity) {
        assertEquals(DESTINATION, ((EditText) activity.findViewById(R.id.destinationEdit)).getText().toString());
        LinearLayout stops = activity.findViewById(R.id.stopsContainer);
        assertEquals(1, stops.getChildCount());
        assertEquals(SHARED_ADDRESS, ((EditText) stops.getChildAt(0).findViewById(R.id.stopEdit)).getText().toString());
    }
}
