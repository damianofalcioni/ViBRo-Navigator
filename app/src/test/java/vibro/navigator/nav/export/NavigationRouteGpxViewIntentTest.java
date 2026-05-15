package vibro.navigator.nav.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class NavigationRouteGpxViewIntentTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void writeExportFile_writesGpxToRouteCache() throws Exception {
        String gpx = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

        File file = NavigationRouteGpxViewIntent.writeExportFile(context, gpx);

        assertEquals(exportFile(), file);
        assertEquals(gpx, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void createForUri_buildsActionViewGpxIntent() {
        Intent intent = NavigationRouteGpxViewIntent.createForUri(
                context,
                Uri.parse("content://vibro.navigator.debug.fileprovider/exports/current-route.gpx")
        );

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(NavigationRouteGpxExporter.GPX_MIME_TYPE, intent.getType());
        assertEquals("content", intent.getData().getScheme());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertNotNull(intent.getClipData());
    }

    @Test
    public void createChooserForIntent_wrapsActionViewSoAndroidShowsAppSelection() {
        Intent actionView = new Intent(Intent.ACTION_VIEW);

        Intent chooser = NavigationRouteGpxViewIntent.createChooserForIntent(context, actionView);

        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        assertEquals(actionView, chooser.getParcelableExtra(Intent.EXTRA_INTENT));
    }

    private File exportFile() {
        return new File(new File(context.getCacheDir(), "exports"), "current-route.gpx");
    }
}
