package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AndroidBRouterStorageInstructionsTest {
    private static final String PROFILES_FOLDER = "profiles2";
    @Test
    public void instructionsPreserveDetectedMediaAndLegacyPaths() {
        String legacy = "Android/data/btools.routingapp/files/brouter/profiles2";
        String media = "Android/media/btools.routingapp/brouter/segments4";
        assertEquals(legacy, AndroidBRouterStorageInstructions.relativePath("primary:" + legacy, PROFILES_FOLDER));
        assertEquals(media, AndroidBRouterStorageInstructions.relativePath("1234-5678:" + media, "segments4"));
    }

    @Test
    public void unavailableOrRootLocationUsesFolderName() {
        assertEquals(PROFILES_FOLDER, AndroidBRouterStorageInstructions.relativePath(null, PROFILES_FOLDER));
        assertEquals(PROFILES_FOLDER, AndroidBRouterStorageInstructions.relativePath("primary:", PROFILES_FOLDER));
        assertEquals("segments4", AndroidBRouterStorageInstructions.relativePath("invalid", "segments4"));
    }
}
