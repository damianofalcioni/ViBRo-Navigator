package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.ui.NavigationActivity;

@RunWith(RobolectricTestRunner.class)
public class MainActivityIntentHandlerTest {

    @Test
    public void redirectGpxImportDuringActiveNavigationOpensNavigationUi() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent gpxIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse("content://example/routes/import.gpx"), "application/gpx+xml");

        boolean handled = MainActivityIntentHandler.redirectGpxImportDuringActiveNavigation(
                activity,
                gpxIntent,
                true
        );

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertTrue(handled);
        assertNotNull(startedIntent);
        assertEquals(NavigationActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertTrue(startedIntent.getBooleanExtra(NavigationActivity.EXTRA_RESUME_EXISTING, false));
    }

    @Test
    public void redirectGpxImportDuringActiveNavigationIgnoresNonGpxIntent() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Intent textIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "48.2082,16.3738");

        boolean handled = MainActivityIntentHandler.redirectGpxImportDuringActiveNavigation(
                activity,
                textIntent,
                true
        );

        assertFalse(handled);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }
}
