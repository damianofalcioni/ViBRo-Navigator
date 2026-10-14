package vibro.navigator.nav.ui;

import android.content.Context;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.android.text.NavigationInstructionSymbolStyle;
import vibro.navigator.nav.format.NavigationInstructionMetricCycle;
import vibro.navigator.nav.format.NavigationInstructionParts;

/** Fixed-width icon and alternating distance/time beside the scrolling instruction. */
public final class NavigationInstructionMetricView extends TextView {
    private final NavigationInstructionMetricCycle cycle = new NavigationInstructionMetricCycle();
    private final Runnable switchMetric = this::refreshPrefix;
    private NavigationInstructionParts parts = NavigationInstructionParts.fromLine("");
    private String instructionKey = "";
    private float reservedTextWidth;

    public NavigationInstructionMetricView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    void setParts(NavigationInstructionParts updatedParts) {
        String updatedKey = updatedParts.symbol + updatedParts.instruction;
        if (!instructionKey.equals(updatedKey)) {
            instructionKey = updatedKey;
            reservedTextWidth = 0f;
        }
        parts = updatedParts;
        refreshPrefix();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float textWidth = Math.max(
                Layout.getDesiredWidth(NavigationInstructionSymbolStyle.apply(parts.distancePrefix, parts.symbol), getPaint()),
                Layout.getDesiredWidth(NavigationInstructionSymbolStyle.apply(parts.timePrefix, parts.symbol), getPaint()));
        reservedTextWidth = Math.max(reservedTextWidth, textWidth);
        int desiredWidth = (int) Math.ceil(reservedTextWidth) + getCompoundPaddingLeft() + getCompoundPaddingRight();
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), getMeasuredHeight());
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        refreshPrefix();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        refreshPrefix();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        refreshPrefix();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(switchMetric);
        cycle.reset();
        super.onDetachedFromWindow();
    }

    private void refreshPrefix() {
        // View visibility callbacks can run before subclass fields are initialized.
        if (parts == null) {
            return;
        }
        removeCallbacks(switchMetric);
        boolean active = isShown() && hasWindowFocus() && getWindowVisibility() == VISIBLE;
        if (!active) {
            cycle.reset();
        }
        long nowMs = AndroidElapsedRealtimeClock.INSTANCE.elapsedRealtimeMs();
        String text = active ? cycle.prefix(parts, nowMs) : parts.distancePrefix;
        if (!TextUtils.equals(getText(), text)) {
            setText(NavigationInstructionSymbolStyle.apply(text, parts.symbol));
        }
        if (active && parts.alternatesMetrics()) {
            postDelayed(switchMetric, cycle.nextSwitchDelayMs(nowMs));
        }
    }
}
