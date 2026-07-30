package vibro.navigator.android.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.regex.Pattern;

public class AndroidRouteGpxAutoSaverTest {
    private static final String GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx />";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void saveToDirectory_writesTimestampedGpxFile() throws Exception {
        File dir = temporaryFolder.newFolder("gpx");

        File file = AndroidRouteGpxAutoSaver.saveToDirectory(dir, GPX, new Date(0L));

        assertEquals(dir, file.getParentFile());
        assertTrue(Pattern.matches("vibro-navigator-route-\\d{14}\\.gpx", file.getName()));
        assertEquals(GPX, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void saveToDirectory_usesCollisionSuffixForSameSecond() throws Exception {
        File dir = temporaryFolder.newFolder("gpx");
        Date now = new Date(0L);

        File first = AndroidRouteGpxAutoSaver.saveToDirectory(dir, GPX, now);
        File second = AndroidRouteGpxAutoSaver.saveToDirectory(dir, GPX, now);

        assertEquals(AndroidRouteGpxAutoSaver.buildFileName(now), first.getName());
        assertEquals(first.getName().replace(".gpx", "-2.gpx"), second.getName());
    }
}
