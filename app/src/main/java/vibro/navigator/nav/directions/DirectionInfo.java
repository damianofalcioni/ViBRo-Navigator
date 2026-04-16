package vibro.navigator.nav.directions;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public final class DirectionInfo {
    @NonNull
    public final String emoji;
    @StringRes
    public final int labelRes;
    public final int exitNumber;
    @NonNull
    public final DirectionKind kind;

    public DirectionInfo(@NonNull String emoji, @StringRes int labelRes, int exitNumber, @NonNull DirectionKind kind) {
        this.emoji = emoji;
        this.labelRes = labelRes;
        this.exitNumber = exitNumber;
        this.kind = kind;
    }
}

