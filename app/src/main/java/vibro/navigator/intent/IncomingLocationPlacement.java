package vibro.navigator.intent;

import java.util.List;

public final class IncomingLocationPlacement {
    private IncomingLocationPlacement() {
    }

    /** Returns -1 for the destination, an existing stop index, or the stop count to append. */
    public static int targetStopIndex(String destinationText, List<String> stopTexts) {
        if (destinationText.trim().isEmpty()) {
            return -1;
        }
        for (int index = 0; index < stopTexts.size(); index++) {
            if (stopTexts.get(index).trim().isEmpty()) {
                return index;
            }
        }
        return stopTexts.size();
    }
}
