package vibro.navigator.nav.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

/** Scrolls only the maneuver description while the adjacent metadata stays fixed. */
public final class NavigationInstructionScrollView extends HorizontalScrollView {
    private static final long END_PAUSE_MS = 1200;
    private static final float SPEED_DP_PER_SECOND = 36f;

    @Nullable
    private ValueAnimator animator;
    private String instruction = "";

    public NavigationInstructionScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        String updatedInstruction = ((TextView) getChildAt(0)).getText().toString();
        if (!instruction.equals(updatedInstruction)) {
            instruction = updatedInstruction;
            stopScrolling();
        }
        updateScrolling();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        updateScrolling();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateScrolling();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateScrolling();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopScrolling();
        super.onDetachedFromWindow();
    }

    private int overflow() {
        if (getChildCount() == 0) {
            return 0;
        }
        return Math.max(0, getChildAt(0).getWidth() - getWidth() + getPaddingLeft() + getPaddingRight());
    }

    private void updateScrolling() {
        int distance = overflow();
        if (!canScroll(distance)) {
            stopScrolling();
            return;
        }
        if (animator != null) {
            return;
        }
        float pixelsPerSecond = SPEED_DP_PER_SECOND * getResources().getDisplayMetrics().density;
        long travelMs = Math.max(1, Math.round(distance * 1000f / pixelsPerSecond));
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(travelMs + END_PAUSE_MS * 2);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            float elapsed = (float) animation.getAnimatedValue() * animation.getDuration();
            float progress = Math.max(0f, Math.min(1f, (elapsed - END_PAUSE_MS) / travelMs));
            scrollTo(Math.round(progress * overflow()), 0);
        });
        animator.start();
    }

    private boolean canScroll(int distance) {
        return distance > 0 && isShown() && hasWindowFocus() && getWindowVisibility() == VISIBLE;
    }

    private void stopScrolling() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        scrollTo(0, 0);
    }
}
