package vibro.navigator.android.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
        packageManager.addServiceIfNotPresent(queriedService);
        packageManager.addIntentFilterForService(
                queriedService,
                intentFilterFor(RecognitionService.SERVICE_INTERFACE)
        );

        assertEquals(queriedService, availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_usesEnabledDistributionFallbackWhenQueryIsEmpty() {
        installPackage(PACKAGE_NAME, true);

        assertEquals(fallbackService, availabilityWithFallback().firstRecognitionService());
    }

    @Test
    public void firstRecognitionService_skipsDisabledDistributionFallbackWhenQueryIsEmpty() {
        installPackage(PACKAGE_NAME, false);

        assertNull(availabilityWithFallback().firstRecognitionService());
    }

    @NonNull
    private AndroidSpeechRecognitionAvailability availabilityWithFallback() {
        return new AndroidSpeechRecognitionAvailability(
                ApplicationProvider.getApplicationContext().getPackageManager(),
                fallbackService
        );
    }

    private void installPackage(@NonNull String packageName, boolean enabled) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        applicationInfo.enabled = enabled;
        packageInfo.applicationInfo = applicationInfo;
        packageManager.installPackage(packageInfo);
    }

    @NonNull
    private static IntentFilter intentFilterFor(@NonNull String action) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        return intentFilter;
    }
}
