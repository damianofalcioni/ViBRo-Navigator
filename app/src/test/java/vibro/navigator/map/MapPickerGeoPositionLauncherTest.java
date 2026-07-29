package vibro.navigator.map;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;

import androidx.core.content.IntentCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class MapPickerGeoPositionLauncherTest {
    private static final double LAT = 48.2082d;
    private static final double LON = 16.3738d;
    private static final String GEO_URI = "geo:48.208200,16.373800?q=48.208200,16.373800(Selected)";

    @Test
    public void open_usesSelectedPoiWhenAvailable() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MapPickerGeoPositionLauncher launcher = new MapPickerGeoPositionLauncher(
                activity,
                new MapPickerScriptController(),
                () -> new Poi("Selected", LAT, LON)
        );

        launcher.open();

        Intent chooser = shadowOf(activity).getNextStartedActivity();
        Intent actionView = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        assertEquals(Intent.ACTION_VIEW, actionView.getAction());
        assertEquals(GEO_URI, actionView.getDataString());
    }
}
