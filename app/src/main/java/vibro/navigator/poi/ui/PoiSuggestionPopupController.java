package vibro.navigator.poi.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;

import androidx.annotation.NonNull;
import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.logging.AppLogger;

final class PoiSuggestionPopupController {

    private static final int MAX_VISIBLE_ROWS = 3;
    private static final int POPUP_ROW_HEIGHT_DP = 56;
    private static final int MIN_VISIBLE_POPUP_HEIGHT_DP = 48;

    interface SelectionListener {
        void onSuggestionSelected(@NonNull PoiSuggestion suggestion);
    }

    private final EditText anchor;
    private final PoiSuggestionAdapter adapter;
    private final ListPopupWindow popup;
    private final String logTag;
    private final int popupRowHeightPx;
    private final int minVisiblePopupHeightPx;
    private final Rect visibleFrame = new Rect();
    private final int[] anchorLocation = new int[2];

    PoiSuggestionPopupController(
            @NonNull Context context,
            @NonNull EditText anchor,
            @NonNull PoiSuggestionAdapter adapter,
            @NonNull String logTag,
            @NonNull SelectionListener listener
    ) {
        this.anchor = anchor;
        this.adapter = adapter;
        this.logTag = logTag;
        this.popupRowHeightPx = dp(context, POPUP_ROW_HEIGHT_DP);
        this.minVisiblePopupHeightPx = dp(context, MIN_VISIBLE_POPUP_HEIGHT_DP);
        this.popup = new ListPopupWindow(context);
        popup.setAnchorView(anchor);
        popup.setAdapter(adapter);
        popup.setModal(false);
        popup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setBackgroundDrawable(new ColorDrawable(
                AndroidAppTheme.color(context, R.attr.vibroBackgroundColor)
        ));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            PoiSuggestion suggestion = (PoiSuggestion) adapter.getItem(position);
            listener.onSuggestionSelected(suggestion);
        });
    }

    void dismiss() {
        if (popup.isShowing()) {
            popup.dismiss();
        }
    }

    void showIfPossible(@NonNull String reason) {
        if (!canShow(reason)) {
            return;
        }
        try {
            configurePopupPlacement();
            popup.show();
            if (popup.getListView() != null) {
                popup.getListView().setItemsCanFocus(true);
            }
        } catch (WindowManager.BadTokenException | IllegalStateException e) {
            AppLogger.w(logTag, "Skipping popup show because anchor window is not ready reason=" + reason, e);
        }
    }

    private void configurePopupPlacement() {
        anchor.getWindowVisibleDisplayFrame(visibleFrame);
        anchor.getLocationOnScreen(anchorLocation);
        int anchorWidth = anchor.getWidth();
        int desiredHeight = desiredPopupHeight(anchorWidth);
        PopupLayout layout = popupLayout(
                visibleFrame.top,
                visibleFrame.bottom,
                anchorLocation[1],
                anchor.getHeight(),
                desiredHeight,
                minVisiblePopupHeightPx
        );
        popup.setWidth(anchorWidth > 0 ? anchorWidth : ListPopupWindow.WRAP_CONTENT);
        popup.setHeight(layout.heightPx);
        popup.setVerticalOffset(layout.verticalOffsetPx);
    }

    private boolean canShow(@NonNull String reason) {
        if (!anchor.hasFocus()) {
            return false;
        }
        boolean attached = anchor.isAttachedToWindow();
        boolean hasWindowToken = anchor.getWindowToken() != null;
        boolean viewVisible = anchor.getVisibility() == View.VISIBLE;
        boolean windowVisible = anchor.getWindowVisibility() == View.VISIBLE;
        if (attached && hasWindowToken && viewVisible && windowVisible) {
            return true;
        }
        AppLogger.d(logTag, "Skipping popup show reason=" + reason
                + " attached=" + attached
                + " windowToken=" + hasWindowToken
                + " viewVisible=" + viewVisible
                + " windowVisible=" + windowVisible);
        return false;
    }

    @NonNull
    static PopupLayout popupLayout(
            int visibleTop,
            int visibleBottom,
            int anchorTop,
            int anchorHeight,
            int itemCount,
            int rowHeightPx,
            int minVisibleHeightPx
    ) {
        return popupLayout(
                visibleTop,
                visibleBottom,
                anchorTop,
                anchorHeight,
                desiredPopupHeight(itemCount, rowHeightPx, 0),
                minVisibleHeightPx
        );
    }

    @NonNull
    static PopupLayout popupLayout(
            int visibleTop,
            int visibleBottom,
            int anchorTop,
            int anchorHeight,
            int desiredHeightPx,
            int minVisibleHeightPx
    ) {
        int safeAnchorHeight = Math.max(0, anchorHeight);
        int anchorBottom = anchorTop + safeAnchorHeight;
        int above = Math.max(0, anchorTop - visibleTop);
        int below = Math.max(0, visibleBottom - anchorBottom);
        if (above >= minVisibleHeightPx) {
            int height = boundedHeight(desiredHeightPx, above);
            return new PopupLayout(height, -(safeAnchorHeight + height));
        }
        return new PopupLayout(boundedHeight(desiredHeightPx, below), 0);
    }

    private int desiredPopupHeight(int popupWidthPx) {
        int itemCount = adapter.getCount();
        return desiredPopupHeight(
                itemCount,
                popupRowHeightPx,
                measuredVisibleContentHeight(popupWidthPx, itemCount)
        );
    }

    static int desiredPopupHeight(
            int itemCount,
            int rowHeightPx,
            int measuredVisibleContentHeightPx
    ) {
        int compactHeight = compactPopupHeight(itemCount, rowHeightPx);
        if (measuredVisibleContentHeightPx > compactHeight) {
            return Math.min(measuredVisibleContentHeightPx, maxPopupHeight(rowHeightPx));
        }
        return compactHeight;
    }

    private int measuredVisibleContentHeight(int popupWidthPx, int itemCount) {
        if (popupWidthPx <= 0 || itemCount <= 0 || itemCount >= MAX_VISIBLE_ROWS) {
            return 0;
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(popupWidthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        FrameLayout parent = new FrameLayout(anchor.getContext());
        View row = null;
        int measuredHeight = 0;
        for (int i = 0; i < itemCount; i++) {
            row = adapter.getView(i, row, parent);
            row.measure(widthSpec, heightSpec);
            measuredHeight += Math.max(1, row.getMeasuredHeight());
        }
        return measuredHeight;
    }

    private static int compactPopupHeight(int itemCount, int rowHeightPx) {
        int visibleRows = Math.max(1, Math.min(itemCount, MAX_VISIBLE_ROWS));
        return visibleRows * Math.max(1, rowHeightPx);
    }

    private static int maxPopupHeight(int rowHeightPx) {
        return MAX_VISIBLE_ROWS * Math.max(1, rowHeightPx);
    }

    private static int boundedHeight(int desiredHeight, int availableHeight) {
        if (availableHeight <= 0) {
            return desiredHeight;
        }
        return Math.max(1, Math.min(desiredHeight, availableHeight));
    }

    private static int dp(@NonNull Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    static final class PopupLayout {
        final int heightPx;
        final int verticalOffsetPx;

        PopupLayout(int heightPx, int verticalOffsetPx) {
            this.heightPx = heightPx;
            this.verticalOffsetPx = verticalOffsetPx;
        }
    }
}
