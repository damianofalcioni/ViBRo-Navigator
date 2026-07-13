package vibro.navigator.android.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.content.IntentCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.regex.Pattern;

import vibro.navigator.nav.export.NavigationRouteGpxExporter;

@RunWith(RobolectricTestRunner.class)
public class AndroidRouteGpxViewIntentTest {
    private static final String GPX_DIR = "gpx";
    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            deleteChildren(new File(externalFilesDir, GPX_DIR));
        }
        deleteChildren(new File(context.getFilesDir(), GPX_DIR));
    }

    @Test
    public void writeExportFile_writesTimestampedGpxToPersistentGpxFolder() throws Exception {
        File file = AndroidRouteGpxViewIntent.writeExportFile(context, GPX, new Date(0L));

        assertEquals(GPX_DIR, file.getParentFile().getName());
        assertTrue(Pattern.matches("vibro-navigator-route-\\d{14}\\.gpx", file.getName()));
        assertEquals(GPX, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void writeExportFile_usesCollisionSuffixForSameSecond() throws Exception {
        Date now = new Date(0L);

        File first = AndroidRouteGpxViewIntent.writeExportFile(context, GPX, now);
        File second = AndroidRouteGpxViewIntent.writeExportFile(context, GPX, now);

        assertEquals(AndroidRouteGpxAutoSaver.buildFileName(now), first.getName());
        assertEquals(first.getName().replace(".gpx", "-2.gpx"), second.getName());
    }

    @Test
    public void autoSave_writesGpxToPersistentGpxFolderWithTimestampedName() throws Exception {
        File file = AndroidRouteGpxAutoSaver.save(context, GPX, new Date(0L));

        assertEquals(GPX_DIR, file.getParentFile().getName());
        assertTrue(Pattern.matches("vibro-navigator-route-\\d{14}\\.gpx", file.getName()));
        assertEquals(GPX, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void autoSave_usesCollisionSuffixForSameSecond() throws Exception {
        Date now = new Date(0L);

        File first = AndroidRouteGpxAutoSaver.save(context, GPX, now);
        File second = AndroidRouteGpxAutoSaver.save(context, GPX, now);

        assertEquals(AndroidRouteGpxAutoSaver.buildFileName(now), first.getName());
        assertEquals(first.getName().replace(".gpx", "-2.gpx"), second.getName());
    }

    @Test
    public void create_buildsActionViewIntentForPersistentGpxFile() throws Exception {
        Intent intent = AndroidRouteGpxViewIntent.create(context, GPX);

        String uri = intent.getData().toString();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertTrue(uri, Pattern.matches(
                "content://[^/]+/(internal|external\\d+)/gpx/vibro-navigator-route-\\d{14}\\.gpx",
                uri
        ));
        assertEquals(NavigationRouteGpxExporter.GPX_MIME_TYPE, intent.getType());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertNotNull(intent.getClipData());
        try (InputStream input = context.getContentResolver().openInputStream(intent.getData())) {
            assertNotNull(input);
            assertEquals(GPX, new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void createForUri_buildsActionViewGpxIntent() {
        Intent intent = AndroidRouteGpxViewIntent.createForUri(
                context,
                Uri.parse("content://vibro.navigator.debug.fileprovider/external0/gpx/vibro-navigator-route-20260713123456.gpx")
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

        Intent chooser = AndroidRouteGpxViewIntent.createChooserForIntent(context, actionView);
        Intent send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);

        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        assertEquals(Intent.ACTION_SEND, send.getAction());
        assertFalse(chooser.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true));
    }

    @Test
    public void createChooserForIntent_addsExplicitViewTargetsWhenGpxHandlersExist() {
        Intent actionView = AndroidRouteGpxViewIntent.createForUri(
                context,
                Uri.parse("content://vibro.navigator.debug.fileprovider/external0/gpx/vibro-navigator-route-20260713123456.gpx")
        );
        registerResolvableIntent(actionView, "com.example.first", "FirstActivity");
        registerResolvableIntent(actionView, "com.example.second", "SecondActivity");

        Intent chooser = AndroidRouteGpxViewIntent.createChooserForIntent(context, actionView);

        Intent primary = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent.class);
        Parcelable[] initialIntents =
                IntentCompat.getParcelableArrayExtra(chooser, Intent.EXTRA_INITIAL_INTENTS, Intent.class);
        assertEquals(Intent.ACTION_SEND, primary.getAction());
        assertEquals(actionView.getData(), IntentCompat.getParcelableExtra(primary, Intent.EXTRA_STREAM, Uri.class));
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

    private static void deleteChildren(@NonNull File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (!child.delete()) {
                throw new AssertionError("Failed to delete " + child.getAbsolutePath());
            }
        }
    }

    private void registerResolvableIntent(@NonNull Intent intent, @NonNull String packageName, @NonNull String name) {
        ShadowPackageManager shadowPackageManager = shadowOf(context.getPackageManager());
        ComponentName component = new ComponentName(packageName, name);
        shadowPackageManager.addActivityIfNotPresent(component);
        shadowPackageManager.addIntentFilterForActivity(component, intentFilterFor(intent));
    }

    @NonNull
    private static IntentFilter intentFilterFor(@NonNull Intent intent) {
        IntentFilter filter = new IntentFilter(intent.getAction());
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        if (intent.getData() != null && intent.getData().getScheme() != null) {
            filter.addDataScheme(intent.getData().getScheme());
        }
        if (intent.getType() != null) {
            try {
                filter.addDataType(intent.getType());
            } catch (IntentFilter.MalformedMimeTypeException e) {
                throw new AssertionError("Invalid test MIME type", e);
            }
        }
        return filter;
    }
}
