package vibro.navigator.settings;

import androidx.annotation.NonNull;

public final class AppPoiCategorySetting {
    @NonNull
    public final String name;
    public final boolean enabled;

    public AppPoiCategorySetting(@NonNull String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
}
