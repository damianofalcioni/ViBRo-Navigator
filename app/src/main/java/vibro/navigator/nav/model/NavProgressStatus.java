package vibro.navigator.nav.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavProgressStatus {
    @NonNull
    public final String destinationLine;
    @NonNull
    public final String stopProgressBlock;
    @NonNull
    public final String detailBlock;
    @Nullable public final NavTravelEstimate travelEstimate;
    @Nullable public final NavStopEstimate nextStop;

    public NavProgressStatus(
            @NonNull String destinationLine,
            @NonNull String stopProgressBlock,
            @NonNull String detailBlock
    ) {
        this(destinationLine, stopProgressBlock, detailBlock, null);
    }

    public NavProgressStatus(@NonNull String destinationLine, @NonNull String stopProgressBlock,
            @NonNull String detailBlock, @Nullable NavTravelEstimate travelEstimate) {
        this(destinationLine, stopProgressBlock, detailBlock, travelEstimate, null);
    }

    public NavProgressStatus(@NonNull String destinationLine, @NonNull String stopProgressBlock,
            @NonNull String detailBlock, @Nullable NavTravelEstimate travelEstimate,
            @Nullable NavStopEstimate nextStop) {
        this.destinationLine = destinationLine;
        this.stopProgressBlock = stopProgressBlock;
        this.detailBlock = detailBlock;
        this.travelEstimate = travelEstimate;
        this.nextStop = nextStop;
    }

    @NonNull
    public String displayStatusBlock() {
        if (!detailBlock.isEmpty()) {
            return detailBlock;
        }
        if (destinationLine.isEmpty()) {
            return stopProgressBlock;
        }
        if (stopProgressBlock.isEmpty()) {
            return destinationLine;
        }
        return destinationLine + "\n" + stopProgressBlock;
    }

    @NonNull
    public NavProgressStatus withDetailBlock(@NonNull String detailBlock) {
        return new NavProgressStatus(destinationLine, stopProgressBlock, detailBlock, travelEstimate, nextStop);
    }
}
