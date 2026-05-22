package vibro.navigator.nav.format;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public interface NavigationTextResources {
    @NonNull
    String getString(@StringRes int resId, Object... formatArgs);

    boolean isImperialUnitsEnabled();
}
