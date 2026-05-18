package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.search.GoogleGeocodeClient;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutGooglePoiApiKeySettingsGplayTest {

    private static final String USER_GOOGLE_POI_API_KEY = "user-key";

    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppSettings.setGooglePoiApiKey(context, "");
        ShadowToast.reset();
    }

    @Test
    public void aboutPageShowsAndSavesUserGooglePoiApiKey() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View container = activity.findViewById(R.id.aboutGooglePoiApiKeyContainer);
        EditText edit = activity.findViewById(R.id.aboutGooglePoiApiKeyEdit);
        Button saveButton = activity.findViewById(R.id.aboutGooglePoiApiKeySaveButton);

        assertEquals(View.VISIBLE, container.getVisibility());

        edit.setText("  " + USER_GOOGLE_POI_API_KEY + "  ");
        saveButton.performClick();

        assertEquals(USER_GOOGLE_POI_API_KEY, AppSettings.getGooglePoiApiKey(activity));
        assertEquals(USER_GOOGLE_POI_API_KEY, edit.getText().toString());
        assertEquals(
                activity.getString(R.string.msg_google_poi_api_key_saved),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void aboutPageDoesNotInitiallyFocusGooglePoiApiKeyField() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View root = activity.findViewById(R.id.aboutRoot);
        EditText edit = activity.findViewById(R.id.aboutGooglePoiApiKeyEdit);

        assertTrue(root.isFocusableInTouchMode());
        assertTrue(root.hasFocus());
        assertFalse(edit.hasFocus());
    }

    @Test
    public void aboutPageClearsUserGooglePoiApiKeyWithEmptyValue() {
        AppSettings.setGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        EditText edit = activity.findViewById(R.id.aboutGooglePoiApiKeyEdit);
        Button saveButton = activity.findViewById(R.id.aboutGooglePoiApiKeySaveButton);

        edit.setText("");
        saveButton.performClick();

        assertEquals("", AppSettings.getGooglePoiApiKey(activity));
        assertEquals(
                activity.getString(R.string.msg_google_poi_api_key_cleared),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void userGooglePoiApiKeyCreatesGoogleSearchClient() {
        AppSettings.setGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);

        PoiSearchClient client = DistributionServices.createGooglePoiSearchClient(context);

        assertTrue(client instanceof GoogleGeocodeClient);
    }

    @Test
    public void missingUserGooglePoiApiKeyDoesNotCreateGoogleSearchClient() {
        PoiSearchClient client = DistributionServices.createGooglePoiSearchClient(context);

        assertNull(client);
    }
}
