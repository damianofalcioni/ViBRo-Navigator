package vibro.navigator.nav.power;

/** Learn behavior from existing observations. No device whitelist, persistence or scheduled work. */
public final class NavigationBatteryBehaviorDetector {
    private static final long MAX_GAP_MS = 600_000L;
    private final NavigationBatteryEvidence evidence = new NavigationBatteryEvidence();
    private NavigationBatteryObservation previous;
    private int direction;
    private long epoch;
    private NavigationBatteryBehavior behavior = NavigationBatteryInterpretation.result(
            NavigationBatteryBehavior.State.UNAVAILABLE, "no_observations", 0);

    public void reset() {
        previous = null;
        direction = 0;
        epoch++;
        behavior = NavigationBatteryInterpretation.result(NavigationBatteryBehavior.State.UNAVAILABLE,
                "reader_reset", epoch);
    }

    public NavigationBatteryBehavior record(NavigationBatteryObservation observation) {
        if (previous != null && previous.sameReading(observation)) {
            return behavior;
        }
        String rejection = rejectionReason(observation);
        if (rejection != null) {
            return restart(observation, rejection);
        }
        if (previous == null) {
            evidence.reset(observation);
        } else {
            rememberDirection(observation);
            evidence.recordStep(previous, observation);
            evidence.record(observation);
        }
        previous = observation;
        NavigationBatteryBehavior resolved = NavigationBatteryInterpretation.resolve(evidence, epoch);
        if (behavior.isInferred() && !sameInterpretation(resolved)) {
            return restart(observation, "interpretation_contradicted");
        }
        behavior = resolved;
        return behavior;
    }

    private String rejectionReason(NavigationBatteryObservation observation) {
        if (observation.powerState != NavigationBatteryObservation.PowerState.DISCHARGING) {
            return observation.powerState == NavigationBatteryObservation.PowerState.CHARGING
                    ? "charging" : "power_state_unknown";
        }
        if (!observation.hasCounter() || observation.elapsedMs < 0) {
            return "unsupported_observation";
        }
        return previous == null ? null : discontinuityReason(observation);
    }

    private String discontinuityReason(NavigationBatteryObservation observation) {
        long gap = observation.elapsedMs - previous.elapsedMs;
        if (gap <= 0 || gap > MAX_GAP_MS) {
            return "observation_time_discontinuity";
        }
        long change = (long) observation.counter - previous.counter;
        if (reversesDirection(change)) {
            return "counter_direction_changed";
        }
        if (!NavigationBatteryInterpretation.plausibleStep(previous, observation, behavior.counterMultiplier)) {
            return "counter_jump";
        }
        return observation.levelIncreasedSince(previous) ? "percentage_increased" : null;
    }

    private boolean reversesDirection(long change) {
        return direction != 0 && change != 0 && Long.signum(change) != direction;
    }

    private void rememberDirection(NavigationBatteryObservation observation) {
        long change = (long) observation.counter - previous.counter;
        if (change != 0) {
            direction = Long.signum(change);
        }
    }

    private NavigationBatteryBehavior restart(NavigationBatteryObservation observation, String reason) {
        epoch++;
        direction = 0;
        boolean usable = observation.hasCounter() && observation.elapsedMs >= 0
                && observation.powerState == NavigationBatteryObservation.PowerState.DISCHARGING;
        previous = usable ? observation : null;
        if (usable) {
            evidence.reset(observation);
        }
        behavior = NavigationBatteryInterpretation.result(usable ? NavigationBatteryBehavior.State.LEARNING
                : NavigationBatteryBehavior.State.UNAVAILABLE, reason, epoch);
        return behavior;
    }

    private boolean sameInterpretation(NavigationBatteryBehavior other) {
        return other.isInferred() && behavior.counterMultiplier == other.counterMultiplier
                && behavior.dischargeDirection == other.dischargeDirection
                && behavior.currentMultiplier == other.currentMultiplier
                && behavior.currentDischargeSign == other.currentDischargeSign;
    }
}
