package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavGuidanceStatus {
    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;

    public NavGuidanceStatus(
            @NonNull String nextLine,
            @NonNull String afterNextLine
    ) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
    }
}
