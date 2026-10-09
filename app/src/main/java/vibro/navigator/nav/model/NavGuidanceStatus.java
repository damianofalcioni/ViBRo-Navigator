package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavGuidanceStatus {
    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    public final boolean nextUncertain;
    public final boolean afterNextUncertain;

    public NavGuidanceStatus(
            @NonNull String nextLine,
            @NonNull String afterNextLine
    ) {
        this(nextLine, afterNextLine, false, false);
    }

    public NavGuidanceStatus(
            @NonNull String nextLine,
            @NonNull String afterNextLine,
            boolean nextUncertain,
            boolean afterNextUncertain
    ) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.nextUncertain = nextUncertain;
        this.afterNextUncertain = afterNextUncertain;
    }

    @NonNull
    public NavGuidanceStatus withDisplayedLines(
            @NonNull String nextLine,
            @NonNull String afterNextLine
    ) {
        return new NavGuidanceStatus(nextLine, afterNextLine);
    }
}
