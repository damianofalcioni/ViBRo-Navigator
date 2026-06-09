package vibro.navigator.logging;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AppLogFileMaintenanceTest {

    @Test
    public void appendBlock_writesUtf8Bytes() throws Exception {
        File dir = Files.createTempDirectory("vibro-log-test").toFile();
        File file = new File(dir, "app.log");
        String message = "direction \u00b0 \u2190\n";

        AppLogFileMaintenance.appendBlock(file, message);

        assertArrayEquals(
                message.getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(file.toPath())
        );
    }
}
