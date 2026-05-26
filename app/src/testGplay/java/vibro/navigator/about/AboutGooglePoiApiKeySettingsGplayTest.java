package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.distribution.GooglePoiApiKeyValidationResult;
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
        AboutGooglePoiApiKeySettings.setApiKeyValidatorForTests(
                new FakeApiKeyValidator(GooglePoiApiKeyValidationResult.VALID)
        );
        ShadowToast.reset();
    }

    @After
    public void tearDown() {
        AboutGooglePoiApiKeySettings.setApiKeyValidatorForTests(null);
    }

    @Test
    public void aboutPageShowsPasswordButtonAndSavesUserGooglePoiApiKey() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View container = activity.findViewById(R.id.aboutGooglePoiApiKeyContainer);
        ImageButton apiKeyButton = activity.findViewById(R.id.aboutGooglePoiApiKeyButton);
        Switch googleSearchSwitch = activity.findViewById(R.id.aboutGooglePoiSearchSwitch);

        assertEquals(View.VISIBLE, container.getVisibility());
        assertFalse(googleSearchSwitch.isEnabled());
        assertFalse(googleSearchSwitch.isChecked());

        apiKeyButton.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText edit = dialog.findViewById(R.id.aboutGooglePoiApiKeyEdit);
        edit.setText("  " + USER_GOOGLE_POI_API_KEY + "  ");
        assertPasswordInput(edit);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(USER_GOOGLE_POI_API_KEY, AppSettings.getGooglePoiApiKey(activity));
        assertTrue(googleSearchSwitch.isEnabled());
        assertTrue(googleSearchSwitch.isChecked());
        assertEquals(
                activity.getString(R.string.msg_google_poi_api_key_saved),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void aboutPageDoesNotInitiallyFocusGooglePoiApiKeyButton() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View root = activity.findViewById(R.id.aboutRoot);
        ImageButton apiKeyButton = activity.findViewById(R.id.aboutGooglePoiApiKeyButton);

        assertTrue(root.isFocusableInTouchMode());
        assertTrue(root.hasFocus());
        assertFalse(apiKeyButton.hasFocus());
    }

    @Test
    public void aboutPageClearsUserGooglePoiApiKeyWithEmptyValue() {
        AppSettings.setValidatedGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton apiKeyButton = activity.findViewById(R.id.aboutGooglePoiApiKeyButton);
        Switch googleSearchSwitch = activity.findViewById(R.id.aboutGooglePoiSearchSwitch);

        apiKeyButton.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText edit = dialog.findViewById(R.id.aboutGooglePoiApiKeyEdit);
        edit.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("", AppSettings.getGooglePoiApiKey(activity));
        assertFalse(googleSearchSwitch.isEnabled());
        assertFalse(googleSearchSwitch.isChecked());
        assertEquals(
                activity.getString(R.string.msg_google_poi_api_key_cleared),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void aboutPageRejectsInvalidGooglePoiApiKey() {
        AboutGooglePoiApiKeySettings.setApiKeyValidatorForTests(
                new FakeApiKeyValidator(GooglePoiApiKeyValidationResult.INVALID)
        );
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        ImageButton apiKeyButton = activity.findViewById(R.id.aboutGooglePoiApiKeyButton);
        Switch googleSearchSwitch = activity.findViewById(R.id.aboutGooglePoiSearchSwitch);

        apiKeyButton.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText edit = dialog.findViewById(R.id.aboutGooglePoiApiKeyEdit);
        edit.setText(USER_GOOGLE_POI_API_KEY);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("", AppSettings.getGooglePoiApiKey(activity));
        assertFalse(googleSearchSwitch.isEnabled());
        assertFalse(googleSearchSwitch.isChecked());
        assertTrue(dialog.isShowing());
        assertEquals(
                activity.getString(R.string.msg_google_poi_api_key_invalid),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void aboutPageGoogleSearchSwitchControlsGoogleSearchClient() {
        AppSettings.setValidatedGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Switch googleSearchSwitch = activity.findViewById(R.id.aboutGooglePoiSearchSwitch);

        assertTrue(googleSearchSwitch.isEnabled());
        assertTrue(googleSearchSwitch.isChecked());

        googleSearchSwitch.performClick();

        assertFalse(AppSettings.isGooglePoiSearchEnabled(activity));
        assertNull(DistributionServices.createGooglePoiSearchClient(activity));

        googleSearchSwitch.performClick();

        assertTrue(AppSettings.isGooglePoiSearchEnabled(activity));
        assertTrue(DistributionServices.createGooglePoiSearchClient(activity) instanceof GoogleGeocodeClient);
    }

    @Test
    public void userGooglePoiApiKeyCreatesGoogleSearchClient() {
        AppSettings.setValidatedGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);

        PoiSearchClient client = DistributionServices.createGooglePoiSearchClient(context);

        assertTrue(client instanceof GoogleGeocodeClient);
    }

    @Test
    public void missingUserGooglePoiApiKeyDoesNotCreateGoogleSearchClient() {
        PoiSearchClient client = DistributionServices.createGooglePoiSearchClient(context);

        assertNull(client);
    }

    @Test
    public void disabledGoogleSearchDoesNotCreateGoogleSearchClient() {
        AppSettings.setValidatedGooglePoiApiKey(context, USER_GOOGLE_POI_API_KEY);
        AppSettings.setGooglePoiSearchEnabled(context, false);

        PoiSearchClient client = DistributionServices.createGooglePoiSearchClient(context);

        assertNull(client);
    }

    private static void assertPasswordInput(EditText edit) {
        int variation = edit.getInputType() & InputType.TYPE_MASK_VARIATION;
        assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD, variation);
        assertTrue(edit.getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    private static final class FakeApiKeyValidator implements AboutGooglePoiApiKeySettings.ApiKeyValidator {
        private final GooglePoiApiKeyValidationResult result;

        private FakeApiKeyValidator(GooglePoiApiKeyValidationResult result) {
            this.result = result;
        }

        @Override
        public void validate(
                String apiKey,
                AboutGooglePoiApiKeySettings.ValidationCallback callback
        ) {
            assertFalse(apiKey.isEmpty());
            callback.onResult(result);
        }
    }
}
