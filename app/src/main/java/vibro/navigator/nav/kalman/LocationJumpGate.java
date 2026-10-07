package vibro.navigator.nav.kalman;

import androidx.annotation.Nullable;
import vibro.navigator.nav.location.NavigationLocation;

/** Quarantine isolated jumps; a second coherent fix permits real relocation and recovery. */
final class LocationJumpGate {
    private static final double UNKNOWN_SPEED_ALLOWANCE_MPS = 55;
    enum Result { ACCEPT, REINITIALIZE, REJECT }
    @Nullable private NavigationLocation pendingJump;

    void reset() {
        pendingJump = null;
    }

    Result evaluate(NavigationLocation previous, NavigationLocation in, double dt) {
        if (!isImplausible(previous, in, dt)) {
            pendingJump = null;
            return Result.ACCEPT;
        }
        boolean confirmed = confirmsPending(in);
        pendingJump = new NavigationLocation(in);
        return confirmed ? Result.REINITIALIZE : Result.REJECT;
    }

    private boolean confirmsPending(NavigationLocation in) {
        return pendingJump != null && in.getElapsedRealtimeOrTimeMs() > pendingJump.getElapsedRealtimeOrTimeMs()
                && !isImplausible(pendingJump, in,
                (in.getElapsedRealtimeOrTimeMs() - pendingJump.getElapsedRealtimeOrTimeMs()) / 1000.0);
    }

    private static boolean isImplausible(NavigationLocation previous, NavigationLocation next, double dt) {
        // Use the actual interval for the travel allowance. Confirmation handles persistent
        // changes when endpoint speeds cannot describe all intervening movement.
        double reportedSpeed = Math.max(LocationFilterPolicy.speed(previous), LocationFilterPolicy.speed(next));
        boolean shortMeasuredInterval = !LocationFilterPolicy.sparseInterval(dt)
                && LocationFilterPolicy.validSpeed(previous) && LocationFilterPolicy.validSpeed(next);
        double speed = shortMeasuredInterval ? reportedSpeed : Math.max(UNKNOWN_SPEED_ALLOWANCE_MPS, reportedSpeed);
        double travel = speed * dt;
        double uncertainty = Math.max(10, 3 * (LocationFilterPolicy.accuracy(previous) + LocationFilterPolicy.accuracy(next)));
        return previous.distanceTo(next) > travel + uncertainty;
    }
}
