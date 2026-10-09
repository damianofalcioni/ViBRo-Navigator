package vibro.navigator.android.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import vibro.navigator.android.logging.AndroidLogFolderWriter;
import vibro.navigator.settings.AppOutputFolderSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class AndroidCustomLogSizeTest {
    @Test
    public void customLogRetainsBoundedHistoryWithoutSplittingUtf8Lines() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        TestOutputDocumentsProvider provider = TestOutputDocumentsProvider.install(context);
        AppOutputFolderSettings.set(context, Kind.LOGS, TestOutputDocumentsProvider.TREE.toString());
        AndroidLogFolderWriter writer = AndroidLogFolderWriter.open(context, "test-log.txt");
        assertNotNull(writer);
        assertTrue(writer.append("initial header\n" + "entrée\n".repeat(700000)));
        assertTrue(writer.append("latest entry\n"));
        writer.close();
        String saved = provider.read(provider.names()[0]);
        assertTrue(saved.startsWith("entrée\n"));
        assertTrue(saved.endsWith("latest entry\n"));
        assertFalse(saved.contains("initial header"));
        assertTrue(saved.length() < 4 * 1024 * 1024);
    }
}
