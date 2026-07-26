package vibro.navigator.logging;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.pm.PackageInfo;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import vibro.navigator.settings.AppAndroidAutoSettings;

@RunWith(RobolectricTestRunner.class)
public class AppLoggerGplayTest {
    private static final String PLAY_SERVICES_PACKAGE = "com.google.android.gms";
    private static final String ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead";

    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        AppLogger.init(context);
    }

    @Test
    public void sessionInfoIncludesGplayRuntimeDetails() throws Exception {
        installPackage(PLAY_SERVICES_PACKAGE, "26.1.12", 2_601_120L);
        installPackage(ANDROID_AUTO_PACKAGE, "15.0", 1_500L);
        AppAndroidAutoSettings.setIntegrationEnabled(context, false);

        assertTrue(AppLogger.setLoggingEnabled(context, true));

        String firstLine = firstLine(readLogContent());
        assertTrue(firstLine.contains("androidAutoSupported=true"));
        assertTrue(firstLine.contains("androidAutoIntegrationEnabled=false"));
        assertTrue(firstLine.contains("androidAutoServiceState="));
        assertTrue(firstLine.contains("androidAutoPhoneHostPackage=" + ANDROID_AUTO_PACKAGE));
        assertTrue(firstLine.contains("androidAutoPhoneHostInstalled=true"));
        assertTrue(firstLine.contains("androidAutoPhoneHostVersionName=15.0"));
        assertTrue(firstLine.contains("googlePlayServicesFlavorSupported=true"));
        assertTrue(firstLine.contains("googlePlayServicesStatus="));
        assertTrue(firstLine.contains("googlePlayServicesPackage=" + PLAY_SERVICES_PACKAGE));
        assertTrue(firstLine.contains("googlePlayServicesInstalled=true"));
        assertTrue(firstLine.contains("googlePlayServicesVersionName=26.1.12"));
        assertTrue(firstLine.contains("googlePlayServicesVersionCode=2601120"));
    }

    private void installPackage(String packageName, String versionName, long versionCode) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.versionName = versionName;
        packageInfo.setLongVersionCode(versionCode);
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        shadowPackageManager.installPackage(packageInfo);
    }

    private String readLogContent() throws Exception {
        return new String(
                Files.readAllBytes(new File(AppLogger.getLogFilePath(context)).toPath()),
                StandardCharsets.UTF_8
        );
    }

    private static String firstLine(String content) {
        int lineEnd = content.indexOf('\n');
        return lineEnd >= 0 ? content.substring(0, lineEnd) : content;
    }
}
