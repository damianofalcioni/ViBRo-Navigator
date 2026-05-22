package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.settings.AppSettings;

public final class AndroidNavigationTextResources implements NavigationTextResources {
    @NonNull
    private final Context context;

    public AndroidNavigationTextResources(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public String getString(@StringRes int resId, Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }

    @Override
    public boolean isImperialUnitsEnabled() {
        return AppSettings.isImperialUnitsEnabled(context);
    }
}
