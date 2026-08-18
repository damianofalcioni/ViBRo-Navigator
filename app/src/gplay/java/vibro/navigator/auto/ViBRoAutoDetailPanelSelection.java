package vibro.navigator.auto;

import android.graphics.RectF;

import androidx.annotation.NonNull;

final class ViBRoAutoDetailPanelSelection {
    private final RectF textColumnBounds = new RectF();
    private ViBRoAutoDetailPanel visibleDetailPanel = ViBRoAutoDetailPanel.NONE;

    void setTextColumnBounds(float left, float top, float width, float height) {
        textColumnBounds.set(left, top, left + width, top + height);
    }

    boolean isVisible() {
        return visibleDetailPanel != ViBRoAutoDetailPanel.NONE;
    }

    @NonNull
    ViBRoAutoDetailPanel visiblePanel() {
        return visibleDetailPanel;
    }

    boolean handleClick(
            float x,
            float y,
            @NonNull RectF gpsStatusBounds,
            @NonNull RectF directionsBounds,
            @NonNull RectF statusBounds
    ) {
        if (closeIfVisible(x, y)) {
            return true;
        }
        return openIfTargeted(x, y, gpsStatusBounds, directionsBounds, statusBounds);
    }

    private boolean closeIfVisible(float x, float y) {
        if (!isVisible()) {
            return false;
        }
        if (!textColumnBounds.contains(x, y)) {
            return false;
        }
        visibleDetailPanel = ViBRoAutoDetailPanel.NONE;
        return true;
    }

    private boolean openIfTargeted(
            float x,
            float y,
            @NonNull RectF gpsStatusBounds,
            @NonNull RectF directionsBounds,
            @NonNull RectF statusBounds
    ) {
        if (gpsStatusBounds.contains(x, y)) {
            toggle(ViBRoAutoDetailPanel.GPS);
            return true;
        }
        if (directionsBounds.contains(x, y)) {
            toggle(ViBRoAutoDetailPanel.DIRECTIONS);
            return true;
        }
        if (statusBounds.contains(x, y)) {
            toggle(ViBRoAutoDetailPanel.TRIP);
            return true;
        }
        return false;
    }

    private void toggle(@NonNull ViBRoAutoDetailPanel detailPanel) {
        visibleDetailPanel = visibleDetailPanel == detailPanel ? ViBRoAutoDetailPanel.NONE : detailPanel;
    }
}
