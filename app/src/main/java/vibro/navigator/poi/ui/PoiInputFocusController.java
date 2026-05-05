package vibro.navigator.poi.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

final class PoiInputFocusController {

    private PoiInputFocusController() {
    }

    static void clearFocusAndHideKeyboard(@NonNull EditText editText) {
        clearFocusAndHideKeyboardNow(editText);
        editText.post(() -> clearFocusAndHideKeyboardNow(editText));
        editText.postDelayed(() -> clearFocusAndHideKeyboardNow(editText), 100L);
    }

    private static void clearFocusAndHideKeyboardNow(@NonNull EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) editText.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
        clearCurrentTextFocus(editText);
        if (parkFocusOnAncestor(editText)) {
            return;
        }
        editText.clearFocus();
    }

    private static void clearCurrentTextFocus(@NonNull EditText editText) {
        View rootView = editText.getRootView();
        if (rootView == null) {
            return;
        }
        View focusedView = rootView.findFocus();
        if (focusedView instanceof EditText) {
            focusedView.clearFocus();
        }
    }

    private static boolean parkFocusOnAncestor(@NonNull EditText editText) {
        ViewParent parent = editText.getParent();
        while (parent instanceof View) {
            View parentView = (View) parent;
            parentView.setFocusableInTouchMode(true);
            if (parentView.requestFocus()) {
                return true;
            }
            parent = parentView.getParent();
        }
        return false;
    }
}
