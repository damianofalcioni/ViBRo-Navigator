package vibro.navigator.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.format.NavigationTripStatsFormatter;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class ViBRoAutoDetailPanelPainter {
    private static final float DETAILS_TEXT_SIZE_SP = 17f;
    private static final float DETAILS_PADDING_DP = 12f;
    private static final float DETAILS_RADIUS_DP = 8f;
    private static final int DETAILS_MAX_LINES = 12;

    @NonNull
    private final CarContext carContext;
    @NonNull
    private final ViBRoAutoSurfaceRenderer.Controls controls;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final TextPaint ellipsizePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    @NonNull
    private final RectF bounds = new RectF();

    ViBRoAutoDetailPanelPainter(
            @NonNull CarContext carContext,
            @NonNull ViBRoAutoSurfaceRenderer.Controls controls,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.carContext = carContext;
        this.controls = controls;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        initPaints();
    }

    void draw(
            @NonNull Canvas canvas,
            @NonNull NavState state,
            float left,
            float top,
            float width,
            float bottom,
            @NonNull ViBRoAutoDetailPanel detailPanel,
            float scale
    ) {
        String[] lines = detailText(detailPanel, state).split("\\n");
        textPaint.setTextSize(sp(DETAILS_TEXT_SIZE_SP, scale));
        textPaint.setTextAlign(Paint.Align.LEFT);
        outlinePaint.setStrokeWidth(dp(1.2f, scale));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent + dp(4f, scale);
        float padding = dp(DETAILS_PADDING_DP, scale);
        float actionTop = bottom - dp(ViBRoAutoButtonRow.BUTTON_SIZE_DP, scale) - dp(14f, scale);
        float maxPanelBottom = top + padding * 2f + lineHeight * (DETAILS_MAX_LINES + 1);
        float panelBottom = Math.max(top, Math.min(actionTop, maxPanelBottom));
        bounds.set(left, top, left + width, panelBottom);
        canvas.drawRoundRect(bounds, dp(DETAILS_RADIUS_DP, scale), dp(DETAILS_RADIUS_DP, scale), fillPaint);
        canvas.drawRoundRect(bounds, dp(DETAILS_RADIUS_DP, scale), dp(DETAILS_RADIUS_DP, scale), outlinePaint);
        drawTitleAndLines(canvas, detailPanel, lines, left, top, width, panelBottom, scale);
    }

    void clearBounds() {
        bounds.setEmpty();
    }

    private void initPaints() {
        textPaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroTextPrimaryColor));
        textPaint.setSubpixelText(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroSurfaceColor));
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(1.2f, 1f));
        outlinePaint.setColor(AndroidAppTheme.color(carContext, R.attr.vibroOutlineColor));
    }

    private void drawTitleAndLines(
            @NonNull Canvas canvas,
            @NonNull ViBRoAutoDetailPanel detailPanel,
            @NonNull String[] lines,
            float left,
            float top,
            float width,
            float bottom,
            float scale
    ) {
        float padding = dp(DETAILS_PADDING_DP, scale);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent + dp(4f, scale);
        float baseline = top + padding - metrics.ascent;
        textPaint.setFakeBoldText(true);
        canvas.drawText(ellipsize(detailTitle(detailPanel), width - padding * 2f), left + padding, baseline, textPaint);
        textPaint.setFakeBoldText(false);
        int lineCount = Math.min(lines.length, availableLineCount(top, bottom, padding, lineHeight));
        for (int i = 0; i < lineCount; i++) {
            canvas.drawText(
                    ellipsize(lines[i], width - padding * 2f),
                    left + padding,
                    baseline + lineHeight * (i + 1),
                    textPaint
            );
        }
    }

    private int availableLineCount(float top, float bottom, float padding, float lineHeight) {
        return Math.max(0, (int) ((bottom - top - padding * 2f) / lineHeight) - 1);
    }

    @NonNull
    private String detailTitle(@NonNull ViBRoAutoDetailPanel detailPanel) {
        switch (detailPanel) {
            case GPS:
                return carContext.getString(R.string.title_nav_gps_details);
            case DIRECTIONS:
                return carContext.getString(R.string.title_nav_directions_details);
            case TRIP:
                return carContext.getString(R.string.title_nav_trip_stats);
            default:
                throw new IllegalArgumentException("Unsupported detail panel=" + detailPanel);
        }
    }

    @NonNull
    private String detailText(@NonNull ViBRoAutoDetailPanel detailPanel, @NonNull NavState state) {
        switch (detailPanel) {
            case GPS:
                return NavigationGpsTelemetryFormatter.formatDetails(
                        carContext,
                        state.gpsStatus.telemetry,
                        nextEvaluationValue(state)
                );
            case DIRECTIONS:
                return controls.buildCurrentDirectionDetailsText();
            case TRIP:
                return NavigationTripStatsFormatter.formatDetails(
                        carContext,
                        state.tripStatus,
                        elapsedRealtimeClock.elapsedRealtimeMs()
                );
            default:
                throw new IllegalArgumentException("Unsupported detail panel=" + detailPanel);
        }
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

    private float dp(float value, float scale) {
        return ViBRoAutoRenderScale.dp(carContext, value, scale);
    }

    private float sp(float value, float scale) {
        return ViBRoAutoRenderScale.sp(carContext, value, scale);
    }
}
