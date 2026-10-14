package vibro.navigator.nav.format;

/** Display parts of the shared symbol/distance - time - instruction format. */
public final class NavigationInstructionParts {
    private static final String SEPARATOR = " - ";

    public final String distancePrefix;
    public final String timePrefix;
    public final String instruction;
    public final String symbol;

    private NavigationInstructionParts(String distancePrefix, String timePrefix, String instruction, String symbol) {
        this.distancePrefix = distancePrefix;
        this.timePrefix = timePrefix;
        this.instruction = instruction;
        this.symbol = symbol;
    }

    public static NavigationInstructionParts fromLine(String line) {
        int firstSeparator = line.indexOf(SEPARATOR);
        int secondSeparator = firstSeparator < 0 ? -1
                : line.indexOf(SEPARATOR, firstSeparator + SEPARATOR.length());
        if (secondSeparator < 0) {
            return new NavigationInstructionParts("", "", line, "");
        }
        String distance = line.substring(0, firstSeparator);
        String symbol = symbolPrefix(distance);
        String time = line.substring(firstSeparator + SEPARATOR.length(), secondSeparator);
        String timePrefix = formattedTimePrefix(symbol, time);
        return new NavigationInstructionParts(distance, timePrefix,
                line.substring(secondSeparator + SEPARATOR.length()), symbol);
    }

    public boolean alternatesMetrics() {
        return !distancePrefix.isEmpty() && !timePrefix.isEmpty();
    }

    private static String symbolPrefix(String distance) {
        int end = distance.indexOf(' ');
        return end < 0 || Character.isDigit(distance.charAt(0)) ? "" : distance.substring(0, end + 1);
    }

    private static String formattedTimePrefix(String symbol, String time) {
        return switch (time) {
            case "", "--", "—" -> "";
            default -> symbol + time;
        };
    }
}
