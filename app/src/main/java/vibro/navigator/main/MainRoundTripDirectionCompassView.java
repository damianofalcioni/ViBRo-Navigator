package vibro.navigator.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.model.RoundTripDirection;

public final class MainRoundTripDirectionCompassView extends View {
    private static final int DEFAULT_SIZE_DP = 56;
    private static final int OUTER_TICK_COUNT = 12;
    private static final float HEADING_ACCURACY_OK_MAX_DEGREES = 25f;
    private static final float HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES = 5f;
    private static final float HEADING_ACCURACY_GUIDE_MAX_DEGREES = 85f;
    private static final float GUIDE_STROKE_WIDTH_DP = 1.2f;
    private static final float CARDINAL_ORBIT_RADIUS_SCALE = 0.44f;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Nullable
    private Float headingDegrees;
    @Nullable
    private Float headingAccuracyDegrees;

    public MainRoundTripDirectionCompassView(@NonNull Context context) {
        super(context);
        init();
    }

    public MainRoundTripDirectionCompassView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MainRoundTripDirectionCompassView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setHeading(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
        this.headingDegrees = headingDegrees == null
                ? null
                : (float) RoundTripDirection.fromHeadingDegrees(headingDegrees);
        this.headingAccuracyDegrees = sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees);
        invalidate();
    }

    public void clearHeading() {
        headingDegrees = null;
        headingAccuracyDegrees = null;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dpInt(DEFAULT_SIZE_DP);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.max(0f, Math.min(cx, cy) - dp(3f));

        surfacePaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassSurfaceColor));
        statusRingPaint.setColor(resolveStatusColor(getContext()));
        tickPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        cardinalPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        headingGuidePaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassMarkColor));
        centerPaint.setColor(AndroidAppTheme.color(getContext(), R.attr.vibroCompassCenterColor));

        canvas.drawCircle(cx, cy, radius, surfacePaint);
        drawHeadingAccuracyGuide(canvas, cx, cy, radius);
        canvas.drawCircle(cx, cy, radius - dp(2f), statusRingPaint);
        drawCompassMarks(canvas, cx, cy, radius);
        drawHeadingGuide(canvas, cx, cy, radius);
        canvas.drawCircle(cx, cy, Math.max(2f, radius * 0.055f), centerPaint);
    }

    boolean isHeadingAccuracyOkForTest() {
        return isHeadingAccuracyOk();
    }

    int statusColorForTest() {
        return resolveStatusColor(getContext());
    }

    @Nullable
    Float headingDegreesForTest() {
        return headingDegrees;
    }

    private void init() {
        setWillNotDraw(false);

        statusRingPaint.setStyle(Paint.Style.STROKE);
        statusRingPaint.setStrokeWidth(dp(GUIDE_STROKE_WIDTH_DP));
        statusRingPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(dp(1.4f));
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        cardinalPaint.setTextAlign(Paint.Align.CENTER);
        cardinalPaint.setFakeBoldText(false);

        headingGuidePaint.setStyle(Paint.Style.STROKE);
        headingGuidePaint.setStrokeWidth(dp(GUIDE_STROKE_WIDTH_DP));
        headingGuidePaint.setStrokeCap(Paint.Cap.ROUND);

        centerPaint.setStyle(Paint.Style.FILL);
    }

    private void drawCompassMarks(@NonNull Canvas canvas, float cx, float cy, float radius) {
        canvas.save();
        canvas.rotate(-(headingDegrees == null ? 0f : headingDegrees), cx, cy);
        for (int i = 0; i < OUTER_TICK_COUNT; i++) {
            float angle = (360f / OUTER_TICK_COUNT) * i - 90f;
            double radians = Math.toRadians(angle);
            float outer = radius * 0.78f;
            float inner = outer - radius * (i % 3 == 0 ? 0.16f : 0.08f);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            canvas.drawLine(cx + inner * cos, cy + inner * sin, cx + outer * cos, cy + outer * sin, tickPaint);
        }
        float cardinalOrbitRadius = cardinalOrbitRadius(radius);
        drawCardinal(canvas, cx, cy - cardinalOrbitRadius, "N", radius);
        drawCardinal(canvas, cx + cardinalOrbitRadius, cy, "O", radius);
        drawCardinal(canvas, cx, cy + cardinalOrbitRadius, "S", radius);
        drawCardinal(canvas, cx - cardinalOrbitRadius, cy, "W", radius);
        canvas.restore();
    }

    private void drawCardinal(@NonNull Canvas canvas, float x, float y, @NonNull String label, float radius) {
        cardinalPaint.setTextSize(Math.max(dp(7f), radius * 0.24f));
        Paint.FontMetrics metrics = cardinalPaint.getFontMetrics();
        canvas.drawText(label, x, y - (metrics.ascent + metrics.descent) / 2f, cardinalPaint);
    }

    private static float cardinalOrbitRadius(float radius) {
        return radius * CARDINAL_ORBIT_RADIUS_SCALE;
    }

    private void drawHeadingGuide(@NonNull Canvas canvas, float cx, float cy, float radius) {
        float top = cy - radius * 0.9f;
        float base = top + radius * 0.22f;
        float halfWidth = radius * 0.13f;
        canvas.drawLine(cx, cy, cx, top, headingGuidePaint);
        canvas.drawLine(cx, top, cx - halfWidth, base, headingGuidePaint);
        canvas.drawLine(cx, top, cx + halfWidth, base, headingGuidePaint);
    }

    private void drawHeadingAccuracyGuide(@NonNull Canvas canvas, float cx, float cy, float radius) {
        Float visibleAccuracyDegrees = visibleHeadingAccuracyDegrees();
        if (visibleAccuracyDegrees == null) {
            return;
        }
        int previousAlpha = statusRingPaint.getAlpha();
        statusRingPaint.setAlpha(180);
        drawHeadingAccuracyGuideLine(canvas, cx, cy, radius, -90f - visibleAccuracyDegrees);
        drawHeadingAccuracyGuideLine(canvas, cx, cy, radius, -90f + visibleAccuracyDegrees);
        statusRingPaint.setAlpha(previousAlpha);
    }

    private void drawHeadingAccuracyGuideLine(
            @NonNull Canvas canvas,
            float cx,
            float cy,
            float radius,
            float angleDegrees
    ) {
        double radians = Math.toRadians(angleDegrees);
        float guideRadius = radius * 0.76f;
        canvas.drawLine(
                cx,
                cy,
                cx + (float) Math.cos(radians) * guideRadius,
                cy + (float) Math.sin(radians) * guideRadius,
                statusRingPaint
        );
    }

    @Nullable
    private Float visibleHeadingAccuracyDegrees() {
        if (headingAccuracyDegrees == null) {
            return null;
        }
        float bounded = Math.min(
                HEADING_ACCURACY_GUIDE_MAX_DEGREES,
                Math.max(0f, headingAccuracyDegrees)
        );
        return bounded <= 0f ? null : Math.max(HEADING_ACCURACY_GUIDE_MIN_VISIBLE_DEGREES, bounded);
    }

    private int resolveStatusColor(@NonNull Context context) {
        return ContextCompat.getColor(context, isHeadingAccuracyOk() ? R.color.success : R.color.danger);
    }

    private boolean isHeadingAccuracyOk() {
        return headingDegrees != null
                && (headingAccuracyDegrees == null
                || headingAccuracyDegrees <= HEADING_ACCURACY_OK_MAX_DEGREES);
    }

    @Nullable
    private static Float sanitizeHeadingAccuracyDegrees(@Nullable Float value) {
        return value != null && Float.isFinite(value) && value > 0f ? value : null;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int dpInt(int value) {
        return Math.round(dp((float) value));
    }

    float statusRingStrokeWidthForTest() {
        return statusRingPaint.getStrokeWidth();
    }

    float headingGuideStrokeWidthForTest() {
        return headingGuidePaint.getStrokeWidth();
    }

    float cardinalOrbitRadiusForTest(float radius) {
        return cardinalOrbitRadius(radius);
    }
}
