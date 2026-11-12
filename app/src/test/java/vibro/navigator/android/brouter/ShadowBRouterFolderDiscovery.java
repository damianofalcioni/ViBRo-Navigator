package vibro.navigator.android.brouter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = AndroidBRouterFolderDiscovery.class, isInAndroidSdk = false)
public final class ShadowBRouterFolderDiscovery {
    @Implementation
    protected static void execute(Runnable work) {
        work.run();
    }

    @Implementation
    protected static void postResult(Runnable callback) {
        callback.run();
    }
}
