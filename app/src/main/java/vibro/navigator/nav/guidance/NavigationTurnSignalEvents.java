package vibro.navigator.nav.guidance;

import java.util.ArrayList;
import java.util.List;

/** Definitive arrival comes only from arrival confirmation, never from an ETA hint signal. */
final class NavigationTurnSignalEvents {
    private NavigationTurnSignalEvents() { }

    static List<NavigationTurnEvent> empty() { return new ArrayList<>(); }

    static List<NavigationTurnEvent> single(TurnEventPlanner.TurnSignal signal) {
        List<NavigationTurnEvent> result = empty();
        append(result, signal);
        return result;
    }

    static List<NavigationTurnEvent> convert(List<TurnEventPlanner.TurnSignal> signals) {
        List<NavigationTurnEvent> events = empty();
        for (TurnEventPlanner.TurnSignal signal : signals) {
            append(events, signal);
        }
        return events;
    }

    private static void append(List<NavigationTurnEvent> events, TurnEventPlanner.TurnSignal signal) {
        if (NavigationTurnManeuverCueState.isSyntheticArrivalHint(signal.hint)) {
            return;
        }
        switch (signal.type) {
            case PASSED:
                events.add(NavigationTurnEvent.passed(signal.hint));
                break;
            case INITIAL:
                events.add(NavigationTurnEvent.initial(signal.hint, signal.distanceMeters, signal.timeSeconds));
                break;
            case IMMINENT:
                events.add(NavigationTurnEvent.imminent(signal.hint, signal.distanceMeters, signal.timeSeconds));
                break;
        }
    }
}
