package vibro.navigator.nav.export;

import androidx.annotation.Nullable;

/** Captured export inputs that can be serialized without reading a live session. */
public interface NavigationRouteGpxSource {
    @Nullable
    String build();
}
