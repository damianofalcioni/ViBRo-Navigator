package vibro.navigator.nav.compass.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import vibro.navigator.nav.orientation.NavigationCompassGestureState;

/** Separates physical double taps from pinch and tilt streams. */
public final class NavigationCompassTouchListener implements View.OnTouchListener {
    private enum Gesture { UNDECIDED, ZOOM, TILT }

    // Crossing this span ratio advances one level, then consumes the rest of the pinch.
    private static final float ZOOM_STEP_SPAN_RATIO = 1.25f;
    private final NavigationCompassGestureState state;
    private final Runnable redraw;
    private final float touchSlop;
    private final NavigationCompassTapListener taps;
    private boolean multiTouch;
    private boolean tracking;
    private Gesture gesture = Gesture.UNDECIDED;
    private float initialSpan;
    private float initialCenterY;
    private float lastCenterY;
    private int firstPointerId;
    private int secondPointerId;

    public NavigationCompassTouchListener(View view, NavigationCompassGestureState state, Runnable redraw) {
        this.state = state;
        this.redraw = redraw;
        touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        taps = new NavigationCompassTapListener(view);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            multiTouch = false;
            tracking = false;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            onPointerDown(view, event);
        }
        if (!multiTouch) {
            if (taps.onTouch(view, event)) {
                view.performClick();
            }
        } else {
            handleMultiTouch(view, event);
        }
        return true;
    }

    private void onPointerDown(View view, MotionEvent event) {
        if (!multiTouch) {
            taps.cancel(view);
            multiTouch = true;
            if (view.getParent() != null) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }
            begin(event);
        } else {
            tracking = false;
        }
    }

    private void begin(MotionEvent event) {
        tracking = event.getPointerCount() == 2;
        if (!tracking) {
            return;
        }
        firstPointerId = event.getPointerId(0);
        secondPointerId = event.getPointerId(1);
        initialSpan = span(event, 0, 1);
        initialCenterY = (event.getY(0) + event.getY(1)) / 2f;
        lastCenterY = initialCenterY;
        gesture = Gesture.UNDECIDED;
    }

    private void handleMultiTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE -> {
                if (tracking && update(view, event)) {
                    redraw.run();
                }
            }
            case MotionEvent.ACTION_POINTER_UP -> tracking = false;
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finish(view);
            default -> { }
        }
    }

    private void finish(View view) {
        tracking = false;
        view.setPressed(false);
        if (view.getParent() != null) {
            view.getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    private boolean update(View view, MotionEvent event) {
        int first = event.findPointerIndex(firstPointerId);
        int second = event.findPointerIndex(secondPointerId);
        if (first < 0 || second < 0) {
            tracking = false;
            return false;
        }
        float currentSpan = span(event, first, second);
        float centerY = (event.getY(first) + event.getY(second)) / 2f;
        chooseGesture(currentSpan, centerY);
        if (gesture == Gesture.ZOOM) {
            return zoom(currentSpan);
        }
        if (gesture == Gesture.TILT) {
            // Half a view-height changes inclination by 75% of the original tilt.
            float delta = (lastCenterY - centerY) * 1.5f / Math.max(1, view.getHeight());
            lastCenterY = centerY;
            return state.tiltBy(delta);
        }
        return false;
    }

    private void chooseGesture(float currentSpan, float centerY) {
        if (gesture != Gesture.UNDECIDED) {
            return;
        }
        float spanChange = Math.abs(currentSpan - initialSpan);
        float verticalTravel = Math.abs(centerY - initialCenterY);
        if (isPinch(spanChange, verticalTravel)) {
            gesture = Gesture.ZOOM;
        } else if (state.isTiltEnabled() && verticalTravel > touchSlop && verticalTravel > spanChange) {
            gesture = Gesture.TILT;
        }
    }

    private boolean isPinch(float spanChange, float verticalTravel) {
        return state.isZoomEnabled() && spanChange > Math.max(touchSlop, initialSpan * 0.12f)
                && spanChange > verticalTravel;
    }

    private boolean zoom(float currentSpan) {
        if (initialSpan <= touchSlop) {
            return false;
        }
        int steps = 0;
        if (currentSpan >= initialSpan * ZOOM_STEP_SPAN_RATIO) {
            steps = 1;
        } else if (currentSpan <= initialSpan / ZOOM_STEP_SPAN_RATIO) {
            steps = -1;
        }
        if (steps == 0) {
            return false;
        }
        tracking = false;
        return state.zoomBy(steps);
    }

    private static float span(MotionEvent event, int first, int second) {
        return (float) Math.hypot(event.getX(first) - event.getX(second), event.getY(first) - event.getY(second));
    }
}
