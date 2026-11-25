package vibro.navigator.nav.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.core.content.ContextCompat;

import vibro.navigator.R;

/** Applies the shared overspeed treatment to the current speed within a GPS status line. */
public final class NavigationGpsSpeedStyle {
    private NavigationGpsSpeedStyle() { }

    public static void apply(Context context, Spannable line, String speedText) {
        if (speedText.isEmpty()) { return; }
        int start = TextUtils.indexOf(line, speedText);
        if (start < 0) { return; }
        int end = start + speedText.length();
        line.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        line.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.compass_route)),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
