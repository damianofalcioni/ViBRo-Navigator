package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationSpeedLimitFormatter;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class ViBRoAutoTextColumnPainter {
    private static final float GPS_STATUS_TEXT_SIZE_SP = 19f;
    private static final float GPS_STATUS_MIN_TOUCH_HEIGHT_DP = 44f;
    private static final float GPS_DETAILS_TEXT_SIZE_SP = 17f;
    private static final float GPS_DETAILS_PADDING_DP = 12f;
    private static final float GPS_DETAILS_RADIUS_DP = 8f;
    private static final int GPS_DETAILS_MAX_LINES = 8;

    private final CarContext carContext;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailsFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailsOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint ellipsizePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final ViBRoAutoButtonRow buttonRow;
    private final RectF gpsStatusBounds = new RectF();
    private final RectF detailsPanelBounds = new RectF();
    private boolean gpsDetailsVisible;

    ViBRoAutoTextColumnPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.carContext = carContext;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        buttonRow = new ViBRoAutoButtonRow(carContext, controls);
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.white));
        textPaint.setSubpixelText(true);
        detailsFillPaint.setStyle(Paint.Style.FILL);
        detailsFillPaint.setColor(ContextCompat.getColor(carContext, R.color.surface_800));
        detailsOutlinePaint.setStyle(Paint.Style.STROKE);
        detailsOutlinePaint.setStrokeWidth(dp(1.2f));
        detailsOutlinePaint.setColor(ContextCompat.getColor(carContext, R.color.outline));
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
        y = drawGpsStatusLine(canvas, state, left, y, width) + dp(14f);
        if (gpsDetailsVisible) {
            drawGpsDetailsPanel(canvas, state, left, y, width, top + height);
            buttonRow.draw(canvas, state, left, top + height, width);
            return;
        }
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
        if (gpsStatusBounds.contains(x, y)) {
            gpsDetailsVisible = !gpsDetailsVisible;
            return true;
        }
        return buttonRow.handleClick(x, y, state);
    }

    private float drawGpsStatusLine(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width
    ) {
        String text = gpsStatusText(state).trim();
        if (text.isEmpty()) {
            gpsStatusBounds.setEmpty();
            return top;
        }
        textPaint.setTextSize(sp(GPS_STATUS_TEXT_SIZE_SP));
        textPaint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = top - metrics.ascent;
        drawGpsStatusText(canvas, state, text, left, baseline, width);
        float bottom = baseline + metrics.descent;
        gpsStatusBounds.set(left, top, left + width, Math.max(bottom, top + dp(GPS_STATUS_MIN_TOUCH_HEIGHT_DP)));
        return bottom;
    }

    private void drawGpsStatusText(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            @NonNull String text,
            float left,
            float baseline,
            float width
    ) {
        String visibleText = ellipsize(text, width);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.white));
        canvas.drawText(visibleText, left, baseline, textPaint);
        drawGpsAccuracy(canvas, state, visibleText, left, baseline);
        if (NavigationSpeedLimitFormatter.isOverLimit(
                state.gpsStatus.telemetry.speedMps,
                state.routeStatus.speedLimit
        )) {
            drawBoldGpsSpeed(canvas, state, visibleText, left, baseline);
        }
        textPaint.setFakeBoldText(false);
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.white));
    }

    private void drawGpsAccuracy(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            @NonNull String visibleText,
            float left,
            float baseline
    ) {
        int accuracyStart = accuracyStart(visibleText, state);
        if (accuracyStart < 0) {
            return;
        }
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.compass_accent));
        canvas.drawText(
                state.gpsStatus.telemetry.accuracyText,
                left + textPaint.measureText(visibleText, 0, accuracyStart),
                baseline,
                textPaint
        );
        textPaint.setColor(ContextCompat.getColor(carContext, R.color.white));
    }

    private void drawBoldGpsSpeed(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            @NonNull String visibleText,
            float left,
            float baseline
    ) {
        String speedText = state.gpsStatus.telemetry.speedText;
        if (!visibleText.startsWith(speedText)) {
            return;
        }
        textPaint.setFakeBoldText(true);
        canvas.drawText(speedText, left, baseline, textPaint);
        textPaint.setFakeBoldText(false);
    }

    private int accuracyStart(@NonNull String visibleText, @NonNull NavState state) {
        int elevationStart = visibleText.indexOf(state.gpsStatus.telemetry.elevationText);
        if (elevationStart < 0) {
            return -1;
        }
        int accuracyStart = visibleText.indexOf(
                state.gpsStatus.telemetry.accuracyText,
                elevationStart + state.gpsStatus.telemetry.elevationText.length()
        );
        if (accuracyStart < 0
                || accuracyStart + state.gpsStatus.telemetry.accuracyText.length() > visibleText.length()) {
            return -1;
        }
        return accuracyStart;
    }

    private void drawGpsDetailsPanel(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float bottom
    ) {
        String[] lines = NavigationGpsTelemetryFormatter.formatDetails(
                carContext,
                state.gpsStatus.telemetry,
                nextEvaluationValue(state)
        ).split("\\n");
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(GPS_DETAILS_TEXT_SIZE_SP));
        textPaint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent + dp(4f);
        float padding = dp(GPS_DETAILS_PADDING_DP);
        float actionTop = bottom - dp(ViBRoAutoButtonRow.BUTTON_SIZE_DP) - dp(14f);
        float panelBottom = Math.max(top, Math.min(actionTop, top + padding * 2f + lineHeight * GPS_DETAILS_MAX_LINES));
        detailsPanelBounds.set(left, top, left + width, panelBottom);
        canvas.drawRoundRect(detailsPanelBounds, dp(GPS_DETAILS_RADIUS_DP), dp(GPS_DETAILS_RADIUS_DP), detailsFillPaint);
        canvas.drawRoundRect(detailsPanelBounds, dp(GPS_DETAILS_RADIUS_DP), dp(GPS_DETAILS_RADIUS_DP), detailsOutlinePaint);
        float baseline = top + padding - metrics.ascent;
        int lineCount = Math.min(lines.length, Math.max(0, (int) ((panelBottom - top - padding * 2f) / lineHeight)));
        for (int i = 0; i < lineCount; i++) {
            canvas.drawText(
                    ellipsize(lines[i], width - padding * 2f),
                    left + padding,
                    baseline + lineHeight * i,
                    textPaint
            );
        }
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
        textPaint.setFakeBoldText(false);
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
        long remainingMs = Math.max(0L, deadline - elapsedRealtimeClock.elapsedRealtimeMs());
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
