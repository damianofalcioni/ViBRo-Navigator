package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Keeps a bounded set of north-up route paths while the viewport and source are unchanged. */
final class NavigationRoutePathCache {
    private static final int MAX_PATHS = 32;

    private final List<Entry> entries = new ArrayList<>();
    private float cx;
    private float cy;
    private float scale;
    private float visibleRadiusMeters;
    private float drawPaddingMeters;
    private boolean hasContext;

    @Nullable
    Entry find(
            @NonNull Object source,
            int sourceSlot,
            int startIndex,
            int endIndex,
            float cx,
            float cy,
            float scale,
            float visibleRadiusMeters,
            float drawPaddingMeters
    ) {
        if (!sameContext(cx, cy, scale, visibleRadiusMeters, drawPaddingMeters)) {
            entries.clear();
            this.cx = cx;
            this.cy = cy;
            this.scale = scale;
            this.visibleRadiusMeters = visibleRadiusMeters;
            this.drawPaddingMeters = drawPaddingMeters;
            hasContext = true;
        }
        for (Entry entry : entries) {
            if (entry.matches(source, sourceSlot, startIndex, endIndex)) {
                return entry;
            }
        }
        return null;
    }

    @NonNull
    Entry remember(
            @NonNull Object source,
            int sourceSlot,
            int startIndex,
            int endIndex,
            @NonNull Path path,
            boolean visible
    ) {
        if (entries.size() == MAX_PATHS) {
            entries.remove(0);
        }
        Entry entry = new Entry(source, sourceSlot, startIndex, endIndex, path, visible);
        entries.add(entry);
        return entry;
    }

    private boolean sameContext(
            float cx,
            float cy,
            float scale,
            float visibleRadiusMeters,
            float drawPaddingMeters
    ) {
        return hasContext && this.cx == cx && this.cy == cy && this.scale == scale
                && this.visibleRadiusMeters == visibleRadiusMeters
                && this.drawPaddingMeters == drawPaddingMeters;
    }

    static final class Entry {
        private final Object source;
        private final int sourceSlot;
        private final int startIndex;
        private final int endIndex;
        private final Path path;
        private final boolean visible;

        private Entry(Object source, int sourceSlot, int startIndex, int endIndex, Path path, boolean visible) {
            this.source = source;
            this.sourceSlot = sourceSlot;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.path = new Path(path);
            this.visible = visible;
        }

        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        boolean matches(Object source, int sourceSlot, int startIndex, int endIndex) {
            // Source identity changes with route state; list equality can traverse and project the full route.
            return this.sourceSlot == sourceSlot && this.startIndex == startIndex
                    && this.endIndex == endIndex && this.source == source;
        }

        void draw(@NonNull Canvas canvas, @NonNull Paint paint) {
            if (visible) {
                canvas.drawPath(path, paint);
            }
        }
    }
}
