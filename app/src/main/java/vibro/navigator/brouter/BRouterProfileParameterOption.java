package vibro.navigator.brouter;

import androidx.annotation.NonNull;

public final class BRouterProfileParameterOption {
    @NonNull
    public final String value;
    @NonNull
    public final String label;

    public BRouterProfileParameterOption(@NonNull String value, @NonNull String label) {
        this.value = value;
        this.label = label;
    }
}
