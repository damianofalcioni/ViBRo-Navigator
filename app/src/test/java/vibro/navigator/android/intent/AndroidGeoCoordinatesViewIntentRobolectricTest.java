package vibro.navigator.android.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidGeoCoordinatesViewIntentRobolectricTest {
    private static final double LAT = 48.2082d;
    private static final double LON = 16.3738d;
    private static final String GEO_URI_WITHOUT_LABEL = "geo:48.208200,16.373800?q=48.208200,16.373800";

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void createChooser_wrapsGeoIntentSoAndroidShowsAppSelection() {
        Intent chooser = AndroidGeoCoordinatesViewIntent.createChooser(context, LAT, LON, null);
        Intent actionView = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);

        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        assertEquals(Intent.ACTION_VIEW, actionView.getAction());
        assertEquals(GEO_URI_WITHOUT_LABEL, actionView.getDataString());
        assertFalse(chooser.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true));
    }
}
