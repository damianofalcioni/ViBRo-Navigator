package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavProgressStatus {
    @NonNull
    public final String destinationLine;
    @NonNull
    public final String stopProgressBlock;
    @NonNull
    public final String detailBlock;

    public NavProgressStatus(
            @NonNull String destinationLine,
            @NonNull String stopProgressBlock,
            @NonNull String detailBlock
    ) {
        this.destinationLine = destinationLine;
        this.stopProgressBlock = stopProgressBlock;
        this.detailBlock = detailBlock;
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
        return new NavProgressStatus(destinationLine, stopProgressBlock, detailBlock);
    }
}
