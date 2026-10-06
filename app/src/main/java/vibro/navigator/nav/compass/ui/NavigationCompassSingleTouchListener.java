package vibro.navigator.nav.compass.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import vibro.navigator.nav.compass.ui.NavigationCompassTouchListener.ViewChangeListener;
import vibro.navigator.nav.orientation.NavigationCompassSwipeSequence;

/** Dispatches double taps and directional swipes without letting a drag complete a tap pair. */
final class NavigationCompassSingleTouchListener {
    private static final float MINIMUM_SWIPE_DP = 48f;
    private final NavigationCompassTapListener taps;
    private final NavigationCompassSwipeSequence swipes;
    private final ViewChangeListener changeView;

    NavigationCompassSingleTouchListener(View view, ViewChangeListener changeView) {
        this.changeView = changeView;
        taps = new NavigationCompassTapListener(view);
        float density = view.getResources().getDisplayMetrics().density;
        float touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        // Require a deliberate stroke even on devices with a large touch slop.
        swipes = new NavigationCompassSwipeSequence(Math.max(MINIMUM_SWIPE_DP * density, touchSlop * 2f));
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View attached) { }

            @Override
            public void onViewDetachedFromWindow(View detached) {
                cancel(detached);
            }
        });
    }

    boolean onTouch(View view, MotionEvent event) {
        int step = trackSwipe(view, event);
        if (step != 0) {
            taps.cancel(view);
            changeView.onSwipe(step);
            return false;
        }
        return taps.onTouch(view, event);
    }

    void cancel(View view) {
        swipes.cancel();
        taps.cancel(view);
        disallowIntercept(view, false);
    }

    private int trackSwipe(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> swipes.down(event.getX(), event.getY());
            case MotionEvent.ACTION_MOVE -> {
                if (swipes.move(event.getX(), event.getY())) {
                    disallowIntercept(view, true);
                }
            }
            case MotionEvent.ACTION_UP -> {
                disallowIntercept(view, false);
                return swipes.up(event.getX(), event.getY());
            }
            case MotionEvent.ACTION_CANCEL -> cancel(view);
            default -> { }
        }
        return 0;
    }

    private static void disallowIntercept(View view, boolean disallow) {
        if (view.getParent() != null) {
            view.getParent().requestDisallowInterceptTouchEvent(disallow);
        }
    }
}
