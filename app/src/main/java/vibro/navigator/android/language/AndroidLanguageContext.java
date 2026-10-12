package vibro.navigator.android.language;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

/** Keeps application and service text current without restarting an active navigation session. */
public final class AndroidLanguageContext extends ContextWrapper {
    private Configuration localizedConfiguration;
    private Resources localizedResources;
    private LayoutInflater inflater;

    public AndroidLanguageContext(@NonNull Context base) {
        super(base);
    }

    @Override
    public synchronized Resources getResources() {
        Configuration desired = AndroidAppLanguage.configuration(getBaseContext());
        if (!desired.equals(localizedConfiguration)) {
            localizedResources = getBaseContext().createConfigurationContext(desired).getResources();
            localizedConfiguration = desired;
        }
        return localizedResources;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (!LAYOUT_INFLATER_SERVICE.equals(name)) {
            return super.getSystemService(name);
        }
        if (inflater == null) {
            inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
        }
        return inflater;
    }
}
