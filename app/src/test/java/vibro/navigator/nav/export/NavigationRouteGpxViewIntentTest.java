package vibro.navigator.nav.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

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
    public void createChooserForIntent_wrapsSendIntentSoAndroidShowsAppSelection() {
        Intent actionView = new Intent(Intent.ACTION_VIEW);

        Intent chooser = NavigationRouteGpxViewIntent.createChooserForIntent(context, actionView);
        Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);

        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        assertEquals(Intent.ACTION_SEND, send.getAction());
        assertFalse(chooser.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true));
    }

    @Test
    public void createChooserForIntent_addsExplicitViewTargetsWhenGpxHandlersExist() {
        Intent actionView = NavigationRouteGpxViewIntent.createForUri(
                context,
                Uri.parse("content://vibro.navigator.debug.fileprovider/exports/current-route.gpx")
        );
        registerResolvableIntent(actionView, "com.example.first", "FirstActivity");
        registerResolvableIntent(actionView, "com.example.second", "SecondActivity");

        Intent chooser = NavigationRouteGpxViewIntent.createChooserForIntent(context, actionView);

        Intent primary = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        Parcelable[] initialIntents = chooser.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        assertEquals(Intent.ACTION_SEND, primary.getAction());
        assertEquals(actionView.getData(), primary.getParcelableExtra(Intent.EXTRA_STREAM));
        assertNotNull(initialIntents);
        assertEquals(2, initialIntents.length);
        assertEquals(
                new ComponentName("com.example.first", "FirstActivity"),
                ((Intent) initialIntents[0]).getComponent()
        );
        assertEquals(
                new ComponentName("com.example.second", "SecondActivity"),
                ((Intent) initialIntents[1]).getComponent()
        );
        assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertNotNull(chooser.getClipData());
        assertFalse(chooser.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true));
    }

    private File exportFile() {
        return new File(new File(context.getCacheDir(), "exports"), "current-route.gpx");
    }

    private void registerResolvableIntent(@NonNull Intent intent, @NonNull String packageName, @NonNull String name) {
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = name;
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }
}
