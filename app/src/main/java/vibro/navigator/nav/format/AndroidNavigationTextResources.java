package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
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

    @NonNull
    @Override
    public String getQuantityString(@PluralsRes int resId, int quantity, Object... formatArgs) {
        return context.getResources().getQuantityString(resId, quantity, formatArgs);
    }

    @Override
    public boolean isImperialUnitsEnabled() {
        return AppSettings.isImperialUnitsEnabled(context);
    }
}
