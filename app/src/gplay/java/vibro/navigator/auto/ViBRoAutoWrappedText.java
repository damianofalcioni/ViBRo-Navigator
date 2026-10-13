package vibro.navigator.auto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoWrappedText {
    private static final int MAX_LINES = 3;

    private ViBRoAutoWrappedText() {
    }

    @NonNull
    static StaticLayout create(@NonNull CharSequence text, @NonNull Paint paint,
            float width, float textSize, float maxHeight) {
        TextPaint layoutPaint = new TextPaint(paint);
        layoutPaint.setTextSize(textSize);
        layoutPaint.setTextAlign(Paint.Align.LEFT);
        int maxLines = Math.max(1, Math.min(MAX_LINES, (int) (maxHeight / layoutPaint.getFontSpacing())));
        return StaticLayout.Builder.obtain(text, 0, text.length(), layoutPaint, Math.max(1, (int) width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
    }

    static float draw(@NonNull Canvas canvas, @NonNull StaticLayout layout, float left, float top) {
        if (layout.getText().length() == 0) {
            return top;
        }
        int saveCount = canvas.save();
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restoreToCount(saveCount);
        return top + layout.getHeight();
    }

    static int height(@NonNull StaticLayout layout) {
        return layout.getText().length() == 0 ? 0 : layout.getHeight();
    }

    @NonNull
    static CharSequence gpsStatus(@NonNull Context context, @NonNull String text, @NonNull NavState state) {
        SpannableString styled = new SpannableString(text);
        int elevationStart = text.indexOf(state.gpsStatus.telemetry.elevationText);
        int accuracyStart = text.indexOf(state.gpsStatus.telemetry.accuracyText,
                elevationStart + state.gpsStatus.telemetry.elevationText.length());
        if (elevationStart >= 0 && accuracyStart >= 0) {
            styled.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.compass_accent)),
                    accuracyStart, accuracyStart + state.gpsStatus.telemetry.accuracyText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String speedText = state.gpsStatus.telemetry.speedText;
        if (text.startsWith(speedText) && NavigationSpeedLimitFormatter.isOverLimit(
                state.gpsStatus.telemetry.speedMps, state.routeStatus.speedLimit)) {
            styled.setSpan(new StyleSpan(Typeface.BOLD), 0, speedText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }
}
