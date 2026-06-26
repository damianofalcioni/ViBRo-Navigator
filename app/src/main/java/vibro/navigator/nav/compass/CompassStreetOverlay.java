package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompassStreetOverlay {
    @NonNull
    public static final CompassStreetOverlay EMPTY = new CompassStreetOverlay(Collections.emptyList());

    @NonNull
    public final List<CompassStreetSegment> segments;

    public CompassStreetOverlay(@NonNull List<CompassStreetSegment> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }
}
