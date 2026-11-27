package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    /** Following-step distance and time measured from the preceding maneuver or the road entry. */
    @Nullable public final NavManeuver followingPreview;

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
        this(nextLine, afterNextLine, nextUncertain, afterNextUncertain, maneuvers, null);
    }

    public NavGuidanceStatus(@NonNull String nextLine, @NonNull String afterNextLine, boolean nextUncertain,
            boolean afterNextUncertain, @NonNull List<NavManeuver> maneuvers, @Nullable NavManeuver followingPreview) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.nextUncertain = nextUncertain;
        this.afterNextUncertain = afterNextUncertain;
        this.maneuvers = Collections.unmodifiableList(new ArrayList<>(maneuvers));
        this.followingPreview = followingPreview;
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
        return new NavGuidanceStatus(nextLine, afterNextLine, nextUncertain, afterNextUncertain, values,
                followingPreview);
    }

    @NonNull
    public NavGuidanceStatus withDirectGuidance(@NonNull String directLine, @NonNull NavManeuver approach) {
        NavManeuver roadPreview = maneuvers.isEmpty() ? null : maneuvers.get(0);
        return new NavGuidanceStatus(directLine, nextLine, false, false,
                Collections.singletonList(approach), roadPreview);
    }
}
