package vibro.navigator.poi.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListPopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.logging.AppLogger;

final class PoiSuggestionPopupController {

    interface SelectionListener {
        void onPoiSelected(@NonNull Poi poi);
    }

    private final EditText anchor;
    private final ListPopupWindow popup;
    private final String logTag;

    PoiSuggestionPopupController(
            @NonNull Context context,
            @NonNull EditText anchor,
            @NonNull PoiSuggestionAdapter adapter,
            @NonNull String logTag,
            @NonNull SelectionListener listener
    ) {
        this.anchor = anchor;
        this.logTag = logTag;
        this.popup = new ListPopupWindow(context);
        popup.setAnchorView(anchor);
        popup.setAdapter(adapter);
        popup.setModal(false);
        popup.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.black)));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            PoiSuggestion suggestion = (PoiSuggestion) adapter.getItem(position);
            listener.onPoiSelected(suggestion.poi);
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
            popup.show();
            if (popup.getListView() != null) {
                popup.getListView().setItemsCanFocus(true);
            }
        } catch (WindowManager.BadTokenException | IllegalStateException e) {
            AppLogger.w(logTag, "Skipping popup show because anchor window is not ready reason=" + reason, e);
        }
    }

    private boolean canShow(@NonNull String reason) {
        if (!anchor.hasFocus()) {
            return false;
        }
        boolean attached = ViewCompat.isAttachedToWindow(anchor);
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
}
