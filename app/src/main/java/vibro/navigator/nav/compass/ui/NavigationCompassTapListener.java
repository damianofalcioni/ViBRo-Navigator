package vibro.navigator.nav.compass.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import vibro.navigator.nav.orientation.NavigationCompassTapSequence;

/** Tracks physical taps and pressed feedback; the owner dispatches the accessible click. */
final class NavigationCompassTapListener {
    private final NavigationCompassTapSequence taps;

    NavigationCompassTapListener(View view) {
        ViewConfiguration configuration = ViewConfiguration.get(view.getContext());
        taps = new NavigationCompassTapSequence(configuration.getScaledTouchSlop(),
                configuration.getScaledDoubleTapSlop(), ViewConfiguration.getDoubleTapTimeout(),
                ViewConfiguration.getLongPressTimeout());
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
        if (!inside(view, event)) {
            cancel(view);
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                taps.down(event.getX(), event.getY(), event.getEventTime());
                view.setPressed(true);
            }
            case MotionEvent.ACTION_MOVE -> {
                taps.move(event.getX(), event.getY());
                view.setPressed(taps.isActive());
            }
            case MotionEvent.ACTION_UP -> {
                view.setPressed(false);
                return taps.up(event.getX(), event.getY(), event.getEventTime());
            }
            case MotionEvent.ACTION_CANCEL -> cancel(view);
            default -> { }
        }
        return false;
    }

    void cancel(View view) {
        taps.cancel();
        view.setPressed(false);
    }

    private static boolean inside(View view, MotionEvent event) {
        return event.getX() >= 0 && event.getY() >= 0
                && event.getX() < view.getWidth() && event.getY() < view.getHeight();
    }
}
