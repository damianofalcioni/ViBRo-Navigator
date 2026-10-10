package vibro.navigator.android.storage;

import android.content.Context;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import java.io.File;

/** Robolectric does not otherwise supply the media directories returned on physical phones. */
@Implements(className = "android.app.ContextImpl", isInAndroidSdk = false)
public class TestOutputMediaDirs extends org.robolectric.shadows.ShadowContextImpl {
    @RealObject
    private Context context;

    @Implementation
    protected File[] getExternalMediaDirs() {
        File primary = primary(context);
        org.robolectric.shadows.ShadowEnvironment.setExternalStorageRemovable(primary, false);
        return new File[]{primary};
    }

    public static File primary(Context context) {
        return new File(context.getCacheDir(), "primary/Android/media/" + context.getPackageName());
    }
}
