package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SurroundingStreetChunkSelection {
    @NonNull
    static final SurroundingStreetChunkSelection EMPTY =
            new SurroundingStreetChunkSelection(Collections.emptyList(), Collections.emptyList());

    @NonNull
    final List<SurroundingStreetChunkKey> displayKeys;
    @NonNull
    final List<SurroundingStreetChunkKey> prefetchKeys;

    SurroundingStreetChunkSelection(
            @NonNull List<SurroundingStreetChunkKey> displayKeys,
            @NonNull List<SurroundingStreetChunkKey> prefetchKeys
    ) {
        this.displayKeys = immutableCopy(displayKeys);
        this.prefetchKeys = immutableCopy(prefetchKeys);
    }

    boolean isEmpty() {
        return displayKeys.isEmpty() && prefetchKeys.isEmpty();
    }

    @NonNull
    private static List<SurroundingStreetChunkKey> immutableCopy(
            @NonNull List<SurroundingStreetChunkKey> values
    ) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
