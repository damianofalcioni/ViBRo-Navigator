package vibro.navigator.android.language;

import android.app.Service;
import android.content.Context;

/** Keeps a running service on the selected app language as preferences change. */
public abstract class AndroidLocalizedService extends Service {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(new AndroidLanguageContext(base));
    }
}
