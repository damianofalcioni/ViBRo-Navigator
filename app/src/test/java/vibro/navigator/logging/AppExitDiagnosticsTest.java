package vibro.navigator.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.net.Uri;
import vibro.navigator.android.storage.TestDownloadsProvider;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class AppExitDiagnosticsTest {
    private Application context;
    private TestDownloadsProvider downloads;
    private int initialLogFileCount;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        downloads = TestDownloadsProvider.install(context);
        AppLogger.init(context);
        AppLogger.setLoggingEnabled(context, false);
        context.getSharedPreferences("app_exit_diagnostics", Application.MODE_PRIVATE)
                .edit().clear().commit();
        AppLogger.init(context);
        initialLogFileCount = logFileCount();
    }

    @Test
    public void activeNavigationReportsSystemExitOnNextStart() throws Exception {
        AppExitDiagnostics.onProcessStart(context, 101, 1000L, (unused, pid, since) -> null);
        AppExitDiagnostics.navigationStarted(context);

        AppExitDiagnostics.onProcessStart(context, 202, 2000L, (unused, pid, since) ->
                new AppProcessExit(1500L, pid, 9, 125, false, "USER_REQUESTED", "forced by system"));

        String content = readLog();
        assertTrue(content.substring(0, content.indexOf('\n')).contains("Log session system info"));
        assertTrue(content.contains("Previous process exited during navigation"));
        assertTrue(content.contains("reason=USER_REQUESTED"));
        assertTrue(content.contains("description=forced by system"));
        assertFalse(AppLogger.isLoggingEnabled(context));
    }

    @Test
    public void missingSystemHistoryDoesNotInventCause() throws Exception {
        AppExitDiagnostics.onProcessStart(context, 101, 1000L, (unused, pid, since) -> null);
        AppExitDiagnostics.navigationStarted(context);

        AppExitDiagnostics.onProcessStart(context, 202, 2000L, (unused, pid, since) -> null);

        assertTrue(readLog().contains("exit cause unavailable"));
    }

    @Test
    public void cleanNavigationStopDoesNotReportAnomaly() {
        AppExitDiagnostics.onProcessStart(context, 101, 1000L, (unused, pid, since) -> null);
        AppExitDiagnostics.navigationStarted(context);
        AppExitDiagnostics.navigationStopped(context);

        AppExitDiagnostics.onProcessStart(context, 202, 2000L, (unused, pid, since) -> null);

        assertEquals(initialLogFileCount, logFileCount());
    }

    @Test
    public void unrelatedNormalExitIsIgnoredButCrashIsReported() throws Exception {
        AppExitDiagnostics.onProcessStart(context, 101, 1000L, (unused, pid, since) -> null);
        AppExitDiagnostics.onProcessStart(context, 202, 2000L, (unused, pid, since) ->
                new AppProcessExit(1500L, pid, 0, 400, false, "LOW_MEMORY", null));
        assertEquals(initialLogFileCount, logFileCount());

        AppExitDiagnostics.onProcessStart(context, 303, 3000L, (unused, pid, since) ->
                new AppProcessExit(2500L, pid, 0, 100, true, "CRASH_NATIVE", null));
        assertTrue(readLog().contains("reason=CRASH_NATIVE"));
    }

    private String readLog() throws Exception {
        return downloads.read(Uri.parse(AppLogger.getLogFilePath(context)));
    }

    private int logFileCount() {
        return downloads.count();
    }
}
