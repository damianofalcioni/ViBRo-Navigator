package vibro.navigator.android.storage;

import static org.junit.Assert.*;
import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import vibro.navigator.android.logging.AndroidLogFolderWriter;
import vibro.navigator.settings.AppOutputStorageSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidCustomLogSizeTest {
    @Test
    public void logRetainsBoundedHistoryWithoutSplittingUtf8Lines() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        org.robolectric.Shadows.shadowOf((android.app.Application) context)
                .grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        AppOutputStorageSettings.disable(context);
        AndroidOutputStorage.resetFailures();
        TestDownloadsProvider provider = TestDownloadsProvider.install(context);
        AndroidLogFolderWriter writer = AndroidLogFolderWriter.open(context, "test-log.txt");
        assertNotNull(writer);
        assertTrue(writer.append("initial header\n" + "entrée\n".repeat(700000)));
        assertTrue(writer.append("latest entry\n"));
        writer.close();
        String saved = writer.path().startsWith("content:")
                ? provider.read(Uri.parse(writer.path())) : Files.readString(new File(writer.path()).toPath());
        assertTrue(saved.startsWith("entrée\n"));
        assertTrue(saved.endsWith("latest entry\n"));
        assertFalse(saved.contains("initial header"));
        assertTrue(saved.length() < 4 * 1024 * 1024);
    }
}
