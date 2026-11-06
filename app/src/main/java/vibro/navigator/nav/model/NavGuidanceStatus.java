package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NavGuidanceStatus {
    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    public final boolean nextUncertain;
    public final boolean afterNextUncertain;
    @NonNull public final List<NavManeuver> maneuvers;

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
        this(nextLine, afterNextLine, nextUncertain, afterNextUncertain, Collections.emptyList());
    }

    public NavGuidanceStatus(@NonNull String nextLine, @NonNull String afterNextLine,
            boolean nextUncertain, boolean afterNextUncertain, @NonNull List<NavManeuver> maneuvers) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.nextUncertain = nextUncertain;
        this.afterNextUncertain = afterNextUncertain;
        this.maneuvers = Collections.unmodifiableList(new ArrayList<>(maneuvers));
    }

    @NonNull
    public NavGuidanceStatus withDisplayedLines(
            @NonNull String nextLine,
            @NonNull String afterNextLine
    ) {
        return new NavGuidanceStatus(nextLine, afterNextLine);
    }

    @NonNull
    public NavGuidanceStatus withManeuvers(@NonNull List<NavManeuver> values) {
        return new NavGuidanceStatus(nextLine, afterNextLine, nextUncertain, afterNextUncertain, values);
    }
}
