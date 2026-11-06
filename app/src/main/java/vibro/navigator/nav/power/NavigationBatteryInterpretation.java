package vibro.navigator.nav.power;

/** Broad phone/tablet capacity bounds separate hypotheses; they are not a calibration. */
final class NavigationBatteryInterpretation {
    private static final double MIN_CAPACITY_MAH = 200;
    private static final double MAX_CAPACITY_MAH = 30_000;

    private NavigationBatteryInterpretation() { }

    static NavigationBatteryBehavior resolve(NavigationBatteryEvidence evidence, long epoch) {
        if (!evidence.ready()) {
            return result(NavigationBatteryBehavior.State.LEARNING, "insufficient_independent_evidence", epoch);
        }
        int multiplier = counterMultiplier(evidence);
        if (multiplier == 0) {
            return result(NavigationBatteryBehavior.State.AMBIGUOUS, "counter_scale_ambiguous", epoch);
        }
        double chargeUah = Math.abs((double) evidence.counterChange()) * multiplier;
        int currentMultiplier = currentMultiplier(evidence.currentRawHours(), chargeUah);
        if (currentMultiplier < 0) {
            return result(NavigationBatteryBehavior.State.AMBIGUOUS, "current_counter_disagree", epoch);
        }
        return new NavigationBatteryBehavior(NavigationBatteryBehavior.State.INFERRED,
                currentMultiplier == 0 ? "percentage_supported" : "percentage_current_supported", epoch,
                multiplier, Long.signum(evidence.counterChange()), currentMultiplier, evidence.currentSign());
    }

    static NavigationBatteryBehavior result(NavigationBatteryBehavior.State state, String reason, long epoch) {
        return new NavigationBatteryBehavior(state, reason, epoch, 0, 0, 0, 0);
    }

    static boolean plausibleStep(NavigationBatteryObservation previous, NavigationBatteryObservation current,
            int multiplier) {
        double changeUah = Math.abs((double) current.counter - previous.counter) * Math.max(1, multiplier);
        long elapsedMs = current.elapsedMs - previous.elapsedMs;
        return NavigationChargeCounterValidation.plausibleCounterChange(changeUah, elapsedMs);
    }

    private static int counterMultiplier(NavigationBatteryEvidence evidence) {
        boolean micro = plausibleCapacity(evidence, 1);
        boolean milli = plausibleCapacity(evidence, 1_000);
        return micro == milli ? 0 : (micro ? 1 : 1_000);
    }

    private static boolean plausibleCapacity(NavigationBatteryEvidence evidence, int multiplier) {
        double capacityMah = Math.abs((double) evidence.counterChange()) * multiplier
                / (10.0 * evidence.percentDrop());
        return evidence.plausibleSteps(multiplier) && capacityMah >= MIN_CAPACITY_MAH && capacityMah <= MAX_CAPACITY_MAH;
    }

    private static int currentMultiplier(double rawHours, double chargeUah) {
        if (!Double.isFinite(rawHours)) {
            return 0;
        }
        double ratio = rawHours / chargeUah;
        if (ratio >= 0.25 && ratio <= 4) {
            return 1;
        }
        return ratio * 1_000 >= 0.25 && ratio * 1_000 <= 4 ? 1_000 : -1;
    }
}
