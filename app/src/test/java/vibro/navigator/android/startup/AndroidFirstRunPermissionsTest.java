package vibro.navigator.android.startup;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class AndroidFirstRunPermissionsTest {
    private final Application app = ApplicationProvider.getApplicationContext();
    private int completions;

    @Before
    public void setUp() {
        shadowOf((LocationManager) app.getSystemService(Activity.LOCATION_SERVICE)).setLocationEnabled(true);
        shadowOf((PowerManager) app.getSystemService(Activity.POWER_SERVICE))
                .setIgnoringBatteryOptimizations(app.getPackageName(), true);
    }

    @Test
    public void modernAndroidRequestsLocationMicrophoneAndNotificationsTogether() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidFirstRunPermissions setup = setup(activity, null);
        setup.start();
        assertArrayEquals(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS},
                shadowOf(activity).getLastRequestedPermission().requestedPermissions);
        assertEquals(0, completions);
        assertTrue(setup.onRequestPermissionsResult(AndroidFirstRunPermissions.REQUEST_RUNTIME));
        assertEquals(1, completions);
        assertFalse(setup.onRequestPermissionsResult(AndroidFirstRunPermissions.REQUEST_RUNTIME));
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.M, Build.VERSION_CODES.P})
    public void legacyAndroidRequestsReadAndWriteStorageWithoutNotificationsPermission() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        setup(activity, null).start();
        assertArrayEquals(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                shadowOf(activity).getLastRequestedPermission().requestedPermissions);
    }

    @Test
    public void existingGrantsIncludingApproximateLocationSkipRuntimePrompt() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        setup(activity, null).start();
        assertNull(shadowOf(activity).getLastRequestedPermission());
        assertEquals(1, completions);
    }

    @Test
    public void pendingRuntimeRequestSurvivesRecreationAndDenialContinues() {
        Activity original = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidFirstRunPermissions first = setup(original, null);
        first.start();
        Bundle state = new Bundle();
        first.saveState(state);
        Activity replacement = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidFirstRunPermissions restored = setup(replacement, state);
        restored.start();
        assertNull(shadowOf(replacement).getLastRequestedPermission());
        assertFalse(restored.onRequestPermissionsResult(9999));
        restored.onRequestPermissionsResult(AndroidFirstRunPermissions.REQUEST_RUNTIME);
        assertEquals(1, completions);
    }

    @Test
    public void disabledLocationAndBatteryPromptsContinueAfterCancellationAndSettingsReturn() {
        shadowOf((LocationManager) app.getSystemService(Activity.LOCATION_SERVICE)).setLocationEnabled(false);
        shadowOf((PowerManager) app.getSystemService(Activity.POWER_SERVICE))
                .setIgnoringBatteryOptimizations(app.getPackageName(), false);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidFirstRunPermissions setup = setup(activity, null);
        setup.start();
        setup.onRequestPermissionsResult(AndroidFirstRunPermissions.REQUEST_RUNTIME);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, completions);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        Intent request = shadowOf(activity).getNextStartedActivityForResult().intent;
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, request.getAction());
        assertEquals("package:" + app.getPackageName(), request.getDataString());
        Bundle state = new Bundle();
        setup.saveState(state);
        AndroidFirstRunPermissions restored = setup(activity, state);
        restored.start();
        assertTrue(restored.onActivityResult(AndroidFirstRunPermissions.REQUEST_SETTINGS, Activity.RESULT_CANCELED, null));
        assertEquals(1, completions);
    }

    private AndroidFirstRunPermissions setup(Activity activity, Bundle state) {
        return new AndroidFirstRunPermissions(activity, state, () -> completions++);
    }
}
