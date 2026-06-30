package vibro.navigator.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import vibro.navigator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
public class AppLoggerTest {

    private Application context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        AppLogger.init(context);
    }

    @Test
    public void enablingLoggingWritesSessionInfoAsFirstEntry() throws Exception {
        assertTrue(AppLogger.setLoggingEnabled(context, true));

        String content = readLogContent();
        String firstLine = firstLine(content);

        assertTrue(firstLine.contains("INFO/AppLogger"));
        assertSessionInfo(firstLine);
        assertTrue(content.contains("Logging enabled"));
        assertTrue(content.indexOf("Log session system info") < content.indexOf("Logging enabled"));
    }

    @Test
    public void startupWithEnabledLoggingWritesFreshSessionInfoFirst() throws Exception {
        assertTrue(AppLogger.setLoggingEnabled(context, true));
        String firstPath = AppLogger.getLogFilePath(context);
        AppLogger.i("AppLoggerTest", "first session marker");

        AppLogger.init(context);

        String secondPath = AppLogger.getLogFilePath(context);
        String content = readLogContent();

        assertNotEquals(firstPath, secondPath);
        assertSessionInfo(firstLine(content));
        assertFalse(content.contains("first session marker"));
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

    private static void assertSessionInfo(String line) {
        assertTrue(line.contains("Log session system info"));
        assertTrue(line.contains("androidVersion=" + Build.VERSION.RELEASE));
        assertTrue(line.contains("androidSdk=" + Build.VERSION.SDK_INT));
        assertTrue(line.contains("appVersion=" + BuildConfig.VERSION_NAME));
        assertTrue(line.contains("versionCode=" + BuildConfig.VERSION_CODE));
        assertTrue(line.contains("applicationId=" + BuildConfig.APPLICATION_ID));
        assertTrue(line.contains("flavor=" + BuildConfig.FLAVOR));
        assertTrue(line.contains("buildType=" + BuildConfig.BUILD_TYPE));
        assertTrue(line.contains("targetSdk="));
        assertTrue(line.contains("manufacturer="));
        assertTrue(line.contains("model="));
        assertTrue(line.contains("logFile="));
    }
}
