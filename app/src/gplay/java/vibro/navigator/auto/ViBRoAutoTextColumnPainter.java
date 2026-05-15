package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavState;

final class ViBRoAutoTextColumnPainter {

    private final CarContext carContext;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint ellipsizePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final ViBRoAutoButtonRow buttonRow;

    ViBRoAutoTextColumnPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls
    ) {
        this.carContext = carContext;
        buttonRow = new ViBRoAutoButtonRow(carContext, controls);
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.white));
        textPaint.setSubpixelText(true);
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height
    ) {
        float y = top;
        y = drawSingleLine(canvas, gpsStatusText(state), left, y, width, 16f) + dp(18f);
        y = drawSingleLine(canvas, state.routeStatus.guidance.nextLine, left, y, width, 24f) + dp(10f);
        drawSingleLine(canvas, state.routeStatus.guidance.afterNextLine, left, y, width, 19f);
        drawStatusBlock(canvas, state.routeStatus.displayStatusBlock(), left, top, width, height);
        buttonRow.draw(canvas, state, left, top + height, width);
    }

    void drawMessage(@NonNull Canvas canvas, @NonNull Rect area, @NonNull String message) {
        textPaint.setTextSize(sp(20f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = area.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(ellipsize(message, area.width() - dp(32f)), area.centerX(), baseline, textPaint);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        return buttonRow.handleClick(x, y, state);
    }

    private float drawSingleLine(
            @NonNull Canvas canvas,
            @NonNull String rawText,
            float left,
            float top,
            float width,
            float textSizeSp
    ) {
        String text = rawText.trim();
        if (text.isEmpty()) {
            return top;
        }
        textPaint.setTextSize(sp(textSizeSp));
        textPaint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = top - metrics.ascent;
        canvas.drawText(ellipsize(text, width), left, baseline, textPaint);
        return baseline + metrics.descent;
    }

    private void drawStatusBlock(
            @NonNull Canvas canvas,
            @NonNull String rawText,
            float left,
            float top,
            float width,
            float height
    ) {
        String[] lines = rawText.trim().split("\\n");
        textPaint.setTextSize(sp(18f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent + dp(5f);
        int lineCount = Math.min(3, lines.length);
        float actionRowTop = top + height - dp(ViBRoAutoButtonRow.BUTTON_SIZE_DP) - dp(12f);
        float baseline = statusBaseline(top, actionRowTop, lineHeight * lineCount, metrics);
        for (int i = 0; i < lineCount; i++) {
            drawStatusLine(canvas, lines[i], left, width, baseline + lineHeight * i);
        }
    }

    private void drawStatusLine(
            @NonNull Canvas canvas,
            @NonNull String rawText,
            float left,
            float width,
            float baseline
    ) {
        String text = rawText.trim();
        if (!text.isEmpty()) {
            canvas.drawText(ellipsize(text, width), left + width / 2f, baseline, textPaint);
        }
    }

    private float statusBaseline(float top, float actionRowTop, float blockHeight, @NonNull Paint.FontMetrics metrics) {
        float centerY = top + (actionRowTop - top) * 0.64f;
        return centerY - blockHeight / 2f - metrics.ascent;
    }

    @NonNull
    private String gpsStatusText(@NonNull NavState state) {
        return carContext.getString(
                R.string.format_nav_gps_status_with_countdown,
                state.gpsStatus.statusLine,
                nextEvaluationValue(state)
        );
    }

    @NonNull
    private String nextEvaluationValue(@NonNull NavState state) {
        long deadline = state.gpsStatus.nextEvaluationDeadlineElapsedMs;
        long remainingMs = Math.max(0L, deadline - SystemClock.elapsedRealtime());
        if (deadline == NavState.NO_DEADLINE || remainingMs <= 0L) {
            return carContext.getString(R.string.nav_status_unavailable);
        }
        long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
        return carContext.getString(R.string.format_nav_next_position_check_value, remainingSeconds);
    }

    @NonNull
    private String ellipsize(@NonNull String text, float widthPx) {
        ellipsizePaint.set(textPaint);
        CharSequence ellipsized = TextUtils.ellipsize(text, ellipsizePaint, Math.max(1f, widthPx), TextUtils.TruncateAt.END);
        return ellipsized.toString();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, carContext.getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, carContext.getResources().getDisplayMetrics());
    }
}
