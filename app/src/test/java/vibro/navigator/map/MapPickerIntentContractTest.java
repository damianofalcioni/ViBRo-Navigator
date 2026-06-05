package vibro.navigator.map;

import static org.junit.Assert.assertNull;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.poi.Poi;

@RunWith(RobolectricTestRunner.class)
public class MapPickerIntentContractTest {
    @Test
    public void parseInitialPoi_rejectsInvalidCoordinates() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = MapPickerIntentContract.createIntent(
                context,
                "Pick",
                new Poi("Invalid", 91.0d, 16.3738d)
        );

        assertNull(MapPickerIntentContract.parseInitialPoi(intent));
    }

    @Test
    public void parseResult_rejectsInvalidCoordinates() {
        Application context = ApplicationProvider.getApplicationContext();
        Intent data = new Intent();
        MapPickerIntentContract.putResult(data, new Poi("Invalid", 48.2082d, 181.0d));

        assertNull(MapPickerIntentContract.parseResult(context, data));
    }
}
