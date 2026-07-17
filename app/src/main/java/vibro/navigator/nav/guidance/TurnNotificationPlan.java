package vibro.navigator.nav.guidance;

final class TurnNotificationPlan {
    private static final double PREPARATORY_IMMINENT_THRESHOLD_SECONDS = 20.0;
    private static final double SINGLE_INSTRUCTION_THRESHOLD_SECONDS = 10.0;
    private static final double VERY_IMMINENT_THRESHOLD_SECONDS = 5.0;

    private static final TurnNotificationPlan DEFAULT = new TurnNotificationPlan(
            VERY_IMMINENT_THRESHOLD_SECONDS,
            PREPARATORY_IMMINENT_THRESHOLD_SECONDS
    );
    private static final TurnNotificationPlan SINGLE_INSTRUCTION = new TurnNotificationPlan(
            SINGLE_INSTRUCTION_THRESHOLD_SECONDS,
            Double.NEGATIVE_INFINITY
    );

    private final double closestAlertThresholdSeconds;
    private final double preparatoryAlertThresholdSeconds;

    private TurnNotificationPlan(double closestAlertThresholdSeconds, double preparatoryAlertThresholdSeconds) {
        this.closestAlertThresholdSeconds = closestAlertThresholdSeconds;
        this.preparatoryAlertThresholdSeconds = preparatoryAlertThresholdSeconds;
    }

    static TurnNotificationPlan from(boolean singleInstructionMode) {
        return singleInstructionMode ? SINGLE_INSTRUCTION : DEFAULT;
    }

    boolean isClosestAlertDue(double timeToNextSeconds) {
        return timeToNextSeconds <= closestAlertThresholdSeconds;
    }

    boolean isPreparatoryAlertDue(double timeToNextSeconds) {
        return timeToNextSeconds <= preparatoryAlertThresholdSeconds;
    }
}
