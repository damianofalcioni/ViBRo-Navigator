package vibro.navigator.poi.ui;

import android.util.TypedValue;
import android.widget.EditText;

import androidx.annotation.NonNull;

public final class PoiInputOverlayPadding {
    private static final float OVERLAY_PADDING_END_DP = 88f;

    private PoiInputOverlayPadding() {
    }

    public static void reserveEnd(@NonNull EditText editText) {
        int reservedEndPadding = Math.max(editText.getPaddingEnd(), dp(editText, OVERLAY_PADDING_END_DP));
        editText.setPaddingRelative(
                editText.getPaddingStart(),
                editText.getPaddingTop(),
                reservedEndPadding,
                editText.getPaddingBottom()
        );
    }

    private static int dp(@NonNull EditText editText, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                editText.getResources().getDisplayMetrics()
        ));
    }
}
