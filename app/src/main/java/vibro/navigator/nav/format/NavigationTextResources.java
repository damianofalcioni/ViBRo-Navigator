package vibro.navigator.nav.format;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

public interface NavigationTextResources {
    @NonNull
    String getString(@StringRes int resId, Object... formatArgs);

    @NonNull
    String getQuantityString(@PluralsRes int resId, int quantity, Object... formatArgs);

    boolean isImperialUnitsEnabled();
}
