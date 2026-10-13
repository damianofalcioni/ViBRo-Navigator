package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.R;
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class ViBRoAutoTextColumnPainter {
    private static final float GPS_STATUS_TEXT_SIZE_SP = 19f;
    private static final float GPS_STATUS_MIN_TOUCH_HEIGHT_DP = 44f;
    private static final float GPS_DETAILS_RADIUS_DP = 8f;
    private static final float FOREGROUND_PANEL_PADDING_DP = 10f;
    private static final int FOREGROUND_PANEL_ALPHA = 220;

    private final CarContext carContext;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint foregroundPanelFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint ellipsizePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final ViBRoAutoButtonRow buttonRow;
    private final ViBRoAutoDetailPanelPainter detailPanelPainter;
    private final ViBRoAutoDetailPanelSelection detailPanelSelection = new ViBRoAutoDetailPanelSelection();
    private final RectF gpsStatusBounds = new RectF();
    private final RectF directionsBounds = new RectF();
    private final RectF statusBounds = new RectF();

    ViBRoAutoTextColumnPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.carContext = carContext;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        buttonRow = new ViBRoAutoButtonRow(carContext, controls);
        detailPanelPainter = new ViBRoAutoDetailPanelPainter(carContext, controls, elapsedRealtimeClock);
        textPaint.setColor(textPrimaryColor());
        textPaint.setSubpixelText(true);
        foregroundPanelFillPaint.setStyle(Paint.Style.FILL);
        foregroundPanelFillPaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroSurfaceColor));
        foregroundPanelFillPaint.setAlpha(FOREGROUND_PANEL_ALPHA);
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float height,
            boolean foregroundPanel,
            float scale
    ) {
        detailPanelSelection.setTextColumnBounds(left, top, width, height);
        if (foregroundPanel) {
            drawForegroundPanelBackground(canvas, left, top, width, height, scale);
        }
        float y = top;
        y = drawGpsStatusLine(canvas, state, left, y, width, scale) + dp(14f, scale);
        if (detailPanelSelection.isVisible()) {
            directionsBounds.setEmpty();
            statusBounds.setEmpty();
            detailPanelPainter.draw(
                    canvas,
                    state,
                    left,
                    y,
                    width,
                    top + height,
                    detailPanelSelection.visiblePanel(),
                    scale
            );
            buttonRow.draw(canvas, state, left, top + height, width, scale);
            return;
        }
        detailPanelPainter.clearBounds();
        textPaint.setColor(textPrimaryColor());
        drawStatusBlock(canvas, state.routeStatus.displayStatusBlock(), left, top, width, height, scale);
        drawDirections(canvas, state, left, y, width, scale);
        buttonRow.draw(canvas, state, left, top + height, width, scale);
    }

    void drawMessage(@NonNull Canvas canvas, @NonNull Rect area, @NonNull String message, float scale) {
        textPaint.setTextSize(sp(20f, scale));
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = area.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(ellipsize(message, area.width() - dp(32f, scale)), area.centerX(), baseline, textPaint);
    }

    boolean handleClick(float x, float y, @NonNull NavState state) {
        if (buttonRow.handleClick(x, y, state)) {
            return true;
        }
        return detailPanelSelection.handleClick(x, y, gpsStatusBounds, directionsBounds, statusBounds);
    }

    private float drawGpsStatusLine(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float scale
    ) {
        String text = gpsStatusText(state).trim();
        if (text.isEmpty()) {
            gpsStatusBounds.setEmpty();
            return top;
        }
        textPaint.setColor(textPrimaryColor());
        StaticLayout layout = ViBRoAutoWrappedText.create(ViBRoAutoWrappedText.gpsStatus(carContext, text, state),
                textPaint, width, sp(GPS_STATUS_TEXT_SIZE_SP, scale), Float.MAX_VALUE);
        float bottom = ViBRoAutoWrappedText.draw(canvas, layout, left, top);
        gpsStatusBounds.set(
                left,
                top,
                left + width,
                Math.max(bottom, top + dp(GPS_STATUS_MIN_TOUCH_HEIGHT_DP, scale))
        );
        return gpsStatusBounds.bottom;
    }

    private void drawDirections(@NonNull Canvas canvas, @NonNull NavState state,
            float left, float top, float width, float scale) {
        float bottom = statusBounds.top - dp(8f, scale);
        float gap = dp(10f, scale);
        float availableHeight = Math.max(0f, bottom - top - gap);
        boolean hasFollowing = !state.routeStatus.guidance.afterNextLine.trim().isEmpty();
        float nextHeight = hasFollowing ? availableHeight / 2f : availableHeight;
        textPaint.setColor(AndroidAppTheme.navigationInstructionColor(carContext, state.routeStatus.guidance.nextUncertain));
        StaticLayout next = ViBRoAutoWrappedText.create(state.routeStatus.guidance.nextLine.trim(),
                textPaint, width, sp(24f, scale), nextHeight);
        textPaint.setColor(AndroidAppTheme.navigationInstructionColor(carContext, state.routeStatus.guidance.afterNextUncertain));
        StaticLayout following = ViBRoAutoWrappedText.create(state.routeStatus.guidance.afterNextLine.trim(),
                textPaint, width, sp(19f, scale), availableHeight - nextHeight);
        float blockHeight = ViBRoAutoWrappedText.height(next) + ViBRoAutoWrappedText.height(following) + gap;
        float directionsTop = top + Math.max(0f, (bottom - top - blockHeight) / 2f);
        float y = ViBRoAutoWrappedText.draw(canvas, next, left, directionsTop) + gap;
        float directionsBottom = ViBRoAutoWrappedText.draw(canvas, following, left, y);
        directionsBounds.set(left, directionsTop, left + width,
                Math.min(bottom, Math.max(directionsBottom, directionsTop + dp(GPS_STATUS_MIN_TOUCH_HEIGHT_DP, scale))));
        textPaint.setColor(textPrimaryColor());
    }

    private void drawStatusBlock(
            @NonNull Canvas canvas,
            @NonNull String rawText,
            float left,
            float top,
            float width,
            float height,
            float scale
    ) {
        String[] lines = rawText.trim().split("\\n");
        textPaint.setTextSize(sp(18f, scale));
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent + dp(5f, scale);
        int lineCount = Math.min(3, lines.length);
        float actionRowTop = top + height - buttonRow.buttonSizePx(scale) - dp(12f, scale);
        float baseline = statusBaseline(top, actionRowTop, lineHeight * lineCount, metrics);
        for (int i = 0; i < lineCount; i++) {
            drawStatusLine(canvas, lines[i], left, width, baseline + lineHeight * i);
        }
        float textTop = baseline + metrics.ascent - dp(8f, scale);
        float textBottom = baseline + lineHeight * Math.max(0, lineCount - 1)
                + metrics.descent + dp(8f, scale);
        statusBounds.set(
                left,
                Math.max(top, textTop),
                left + width,
                Math.min(actionRowTop, Math.max(textBottom, textTop + dp(GPS_STATUS_MIN_TOUCH_HEIGHT_DP, scale)))
        );
    }

    private void drawStatusLine(
            @NonNull Canvas canvas,
            @NonNull String rawText,
            float left,
            float width,
            float baseline
    ) {
        String text = rawText.trim();
        canvas.drawText(ellipsize(text, width), left + width / 2f, baseline, textPaint);
    }

    private float statusBaseline(float top, float actionRowTop, float blockHeight, @NonNull Paint.FontMetrics metrics) {
        return Math.max(top, actionRowTop - blockHeight) - metrics.ascent;
    }

    @NonNull
    private String gpsStatusText(@NonNull NavState state) {
        return AndroidAppLanguage.getString(carContext,
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
            return AndroidAppLanguage.getString(carContext, R.string.nav_status_unavailable);
        }
        long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
        return AndroidAppLanguage.getString(carContext, R.string.format_nav_next_position_check_value, remainingSeconds);
    }

    private void drawForegroundPanelBackground(
            @NonNull Canvas canvas,
            float left,
            float top,
            float width,
            float height,
            float scale
    ) {
        float padding = dp(FOREGROUND_PANEL_PADDING_DP, scale);
        float radius = dp(GPS_DETAILS_RADIUS_DP, scale);
        canvas.drawRoundRect(
                left - padding,
                top - padding,
                left + width + padding,
                top + height + padding,
                radius,
                radius,
                foregroundPanelFillPaint
        );
    }

    @NonNull
    private String ellipsize(@NonNull String text, float widthPx) {
        ellipsizePaint.set(textPaint);
        CharSequence ellipsized = TextUtils.ellipsize(text, ellipsizePaint, Math.max(1f, widthPx), TextUtils.TruncateAt.END);
        return ellipsized.toString();
    }

    private float dp(float value, float scale) {
        return ViBRoAutoRenderScale.dp(carContext, value, scale);
    }

    private float sp(float value, float scale) {
        return ViBRoAutoRenderScale.sp(carContext, value, scale);
    }

    private int textPrimaryColor() {
        return AndroidAppTheme.color(carContext, R.attr.vibroTextPrimaryColor);
    }

}
