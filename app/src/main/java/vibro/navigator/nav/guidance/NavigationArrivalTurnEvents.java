package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.nav.route.VoiceHint;

public final class NavigationArrivalTurnEvents {
    public static final int DESTINATION_ARRIVAL_COMMAND = 100;
    public static final int INTERMEDIATE_ARRIVAL_COMMAND = 101;

    private NavigationArrivalTurnEvents() {
    }

    @NonNull
    public static List<NavigationTurnEvent> destinationArrival(int trackIndex) {
        return arrival(trackIndex, DESTINATION_ARRIVAL_COMMAND);
    }

    @NonNull
    public static List<NavigationTurnEvent> intermediateArrival(int trackIndex) {
        return arrival(trackIndex, INTERMEDIATE_ARRIVAL_COMMAND);
    }

    @NonNull
    private static List<NavigationTurnEvent> arrival(int trackIndex, int command) {
        VoiceHint arrivalHint = new VoiceHint(trackIndex, command, 0, 0.0, 0);
        List<NavigationTurnEvent> events = new ArrayList<>(1);
        events.add(NavigationTurnEvent.imminent(arrivalHint, 0.0, 0.0));
        return events;
    }
}
