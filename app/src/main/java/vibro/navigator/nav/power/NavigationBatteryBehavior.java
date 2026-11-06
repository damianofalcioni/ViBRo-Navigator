package vibro.navigator.nav.power;

/** Inference is explicitly an estimate, never independent verification of a fuel gauge. */
public final class NavigationBatteryBehavior {
    public enum State { LEARNING, AMBIGUOUS, INFERRED, UNAVAILABLE }

    public final State state;
    public final String reason;
    public final long epoch;
    public final int counterMultiplier;
    public final int dischargeDirection;
    public final int currentMultiplier;
    public final int currentDischargeSign;

    NavigationBatteryBehavior(State state, String reason, long epoch, int counterMultiplier,
            int dischargeDirection, int currentMultiplier, int currentDischargeSign) {
        this.state = state;
        this.reason = reason;
        this.epoch = epoch;
        this.counterMultiplier = counterMultiplier;
        this.dischargeDirection = dischargeDirection;
        this.currentMultiplier = currentMultiplier;
        this.currentDischargeSign = currentDischargeSign;
    }

    public boolean isInferred() { return state == State.INFERRED; }

    public String describe() {
        return "batteryBehavior=" + state + " batteryBehaviorReason=" + reason + " batteryEpoch=" + epoch
                + " counterToUah=" + counterMultiplier + " counterDischargeDirection=" + dischargeDirection
                + " currentToUa=" + currentMultiplier + " currentDischargeSign=" + currentDischargeSign
                + " batteryCalibration=not_independently_verified";
    }
}
