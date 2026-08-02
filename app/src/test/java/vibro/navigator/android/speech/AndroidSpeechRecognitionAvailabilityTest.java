package vibro.navigator.android.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.speech.RecognizerIntent;
import android.speech.RecognitionService;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class AndroidSpeechRecognitionAvailabilityTest {

    private static final String PACKAGE_NAME = "com.example.recognizer";
    private static final String SERVICE_NAME = "com.example.recognizer.RecognitionService";

    private ShadowPackageManager packageManager;
    private ComponentName fallbackService;

    @Before
    public void setUp() {
        packageManager = shadowOf(ApplicationProvider.getApplicationContext().getPackageManager());
        fallbackService = new ComponentName(PACKAGE_NAME, SERVICE_NAME);
    }

    @Test
    public void firstRecognitionService_prefersQueriedService() {
        ComponentName queriedService = new ComponentName("com.example.default", "DefaultRecognitionService");
        addRecognitionService(queriedService, true);

        assertEquals(queriedService, availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_skipsQueriedServiceWithoutMicrophonePermission() {
        ComponentName deniedService = new ComponentName("com.example.denied", "DeniedRecognitionService");
        ComponentName grantedService = new ComponentName("com.example.granted", "GrantedRecognitionService");
        addRecognitionService(deniedService, false);
        addRecognitionService(grantedService, true);

        assertEquals(grantedService, availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_usesEnabledDistributionFallbackWhenQueryIsEmpty() {
        installPackage(PACKAGE_NAME, true, true);

        assertEquals(fallbackService, availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_skipsEnabledDistributionFallbackWithoutMicrophonePermission() {
        installPackage(PACKAGE_NAME, true, false);

        assertNull(availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_skipsDisabledDistributionFallbackWhenQueryIsEmpty() {
        installPackage(PACKAGE_NAME, false);

        assertNull(availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void hasRecognitionProvider_returnsTrueWhenRecognizerActivityExists() {
        ComponentName activity = new ComponentName("com.example.default", "DefaultRecognizerActivity");
        packageManager.addActivityIfNotPresent(activity);
        packageManager.addIntentFilterForActivity(
                activity,
                intentFilterFor(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        );

        assertTrue(availabilityWithFallback().hasRecognitionProvider());
    }

    @Test
    public void hasRecognitionProvider_returnsTrueWhenFallbackServiceIsEnabled() {
        installPackage(PACKAGE_NAME, true, true);

        assertTrue(availabilityWithFallback().hasRecognitionProvider());
    }

    @Test
    public void hasRecognitionProvider_returnsFalseWhenOnlyServiceHasNoMicrophonePermission() {
        addRecognitionService(new ComponentName("com.example.denied", "DeniedRecognitionService"), false);

        assertFalse(availabilityWithFallback().hasRecognitionProvider());
    }

    @Test
    public void hasRecognitionProvider_returnsFalseWhenNoQueriedOrFallbackProviderExists() {
        assertFalse(availabilityWithFallback().hasRecognitionProvider());
    }

    @NonNull
    private AndroidSpeechRecognitionAvailability availabilityWithFallback() {
        return new AndroidSpeechRecognitionAvailability(
                ApplicationProvider.getApplicationContext().getPackageManager(),
                fallbackService
        );
    }

    private void installPackage(@NonNull String packageName, boolean enabled) {
        installPackage(packageName, enabled, false);
    }

    private void installPackage(
            @NonNull String packageName,
            boolean enabled,
            boolean microphonePermissionGranted
    ) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        applicationInfo.enabled = enabled;
        packageInfo.applicationInfo = applicationInfo;
        if (microphonePermissionGranted) {
            packageInfo.requestedPermissions = new String[]{Manifest.permission.RECORD_AUDIO};
            packageInfo.requestedPermissionsFlags = new int[]{PackageInfo.REQUESTED_PERMISSION_GRANTED};
        }
        packageManager.installPackage(packageInfo);
    }

    private void addRecognitionService(
            @NonNull ComponentName service,
            boolean microphonePermissionGranted
    ) {
        installPackage(service.getPackageName(), true, microphonePermissionGranted);
        packageManager.addServiceIfNotPresent(service);
        packageManager.addIntentFilterForService(
                service,
                intentFilterFor(RecognitionService.SERVICE_INTERFACE)
        );
    }

    @NonNull
    private static IntentFilter intentFilterFor(@NonNull String action) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        return intentFilter;
    }
}
