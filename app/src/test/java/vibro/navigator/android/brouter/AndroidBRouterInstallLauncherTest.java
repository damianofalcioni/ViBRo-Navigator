package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import vibro.navigator.brouter.BRouterProfilesRepository;

public class AndroidBRouterInstallLauncherTest {
    private static final String ACTION_VIEW = "android.intent.action.VIEW";
    private static final String MARKET_URI =
            "market://details?id=" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
    private static final String PLAY_STORE_WEB_URI =
            "https://play.google.com/store/apps/details?id="
                    + BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
    private static final String FDROID_URI =
            "https://f-droid.org/packages/" + BRouterProfilesRepository.BROUTER_PACKAGE_NAME + "/";

    @Test
    public void launchPlayStore_usesMarketUriWhenResolvable() {
        RecordingLauncher launcher = new RecordingLauncher().resolves(MARKET_URI);

        assertTrue(AndroidBRouterInstallLauncher.launchPlayStore(launcher));

        assertEquals(ACTION_VIEW, launcher.launchedAction);
        assertEquals(MARKET_URI, launcher.launchedUri);
    }

    @Test
    public void launchPlayStore_fallsBackToWebWhenMarketUriIsUnresolvable() {
        RecordingLauncher launcher = new RecordingLauncher().resolves(PLAY_STORE_WEB_URI);

        assertTrue(AndroidBRouterInstallLauncher.launchPlayStore(launcher));

        assertEquals(ACTION_VIEW, launcher.launchedAction);
        assertEquals(PLAY_STORE_WEB_URI, launcher.launchedUri);
    }

    @Test
    public void launchFdroid_opensFdroidPackagePage() {
        RecordingLauncher launcher = new RecordingLauncher().resolves(FDROID_URI);

        assertTrue(AndroidBRouterInstallLauncher.launchFdroid(launcher));

        assertEquals(ACTION_VIEW, launcher.launchedAction);
        assertEquals(FDROID_URI, launcher.launchedUri);
    }

    @Test
    public void launchPlayStore_returnsFalseWhenNoStorePageCanBeOpened() {
        RecordingLauncher launcher = new RecordingLauncher();

        assertFalse(AndroidBRouterInstallLauncher.launchPlayStore(launcher));

        assertNull(launcher.launchedUri);
    }

    private static final class RecordingLauncher implements AndroidBRouterInstallLauncher.InstallLauncher {
        @NonNull
        private final Set<String> resolvableUris = new HashSet<>();
        private String launchedAction;
        private String launchedUri;

        @NonNull
        RecordingLauncher resolves(@NonNull String uri) {
            resolvableUris.add(uri);
            return this;
        }

        @Override
        public boolean canResolve(@NonNull AndroidBRouterInstallLauncher.InstallTarget target) {
            return resolvableUris.contains(target.uriString);
        }

        @Override
        public boolean launch(@NonNull AndroidBRouterInstallLauncher.InstallTarget target) {
            launchedAction = target.action;
            launchedUri = target.uriString;
            return true;
        }
    }
}
