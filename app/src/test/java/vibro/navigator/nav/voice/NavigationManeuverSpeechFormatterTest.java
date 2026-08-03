package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public class NavigationManeuverSpeechFormatterTest {
    private static final TestNavigationTextResources RESOURCES = TestNavigationTextResources.metric();

    @Test
    public void formatTurnSpeech_readsCountdownBeforeDirection() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                RESOURCES,
                new VoiceHint(0, 2, 0, 0.0, 0),
                20.0
        );

        assertEquals("20 seconds, turn left", message);
    }

    @Test
    public void formatTurnSpeech_includesRoundaboutExit() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                RESOURCES,
                new VoiceHint(0, 13, 3, 0.0, 0),
                65.0
        );

        assertEquals("1 minute, roundabout, exit 3", message);
    }

    @Test
    public void formatTurnSpeech_usesSingularSeconds() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                RESOURCES,
                new VoiceHint(0, 2, 0, 0.0, 0),
                1.0
        );

        assertEquals("1 second, turn left", message);
    }

    @Test
    public void formatTurnSpeech_usesPluralMinutes() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                RESOURCES,
                new VoiceHint(0, 5, 0, 0.0, 0),
                121.0
        );

        assertEquals("2 minutes, turn right", message);
    }

    @Test
    public void formatTurnSpeech_formatsReachedArrivalWithoutCountdown() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                RESOURCES,
                new VoiceHint(0, 100, 0, 0.0, 0),
                0.0
        );

        assertEquals("destination reached", message);
    }

    @Test
    public void formatStationaryOrientationSpeech_roundsTurnDegreesToNearestTen() {
        String left = NavigationManeuverSpeechFormatter.formatStationaryOrientationSpeech(
                RESOURCES,
                new StationaryOrientationAdvisor.Decision(-42.0)
        );
        String right = NavigationManeuverSpeechFormatter.formatStationaryOrientationSpeech(
                RESOURCES,
                new StationaryOrientationAdvisor.Decision(36.0)
        );

        assertEquals("Turn 40 degrees left", left);
        assertEquals("Turn 40 degrees right", right);
    }

    @Test
    public void formatOffRouteSpeech_distinguishesRecalculationAndNoticeOnly() {
        NavigationRerouteNotice notice = NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(25.0, 8f, 90.0, 90.0)
        );

        assertEquals(
                "Off route. Recalculating",
                NavigationManeuverSpeechFormatter.formatOffRouteSpeech(RESOURCES, notice)
        );
        assertEquals(
                "Off route",
                NavigationManeuverSpeechFormatter.formatOffRouteSpeech(RESOURCES, notice.asNotificationOnly())
        );
    }

    @Test
    public void formatOffRouteSpeech_readsNoticeOnlyBearingMismatchAsWrongDirection() {
        NavigationRerouteNotice notice = NavigationRerouteNotice.fromDecision(
                new RouteDeviationPolicy().evaluate(5.0, 5f, 90.0, 180.0)
        ).asNotificationOnly();

        assertEquals(
                "Wrong direction. Turn back",
                NavigationManeuverSpeechFormatter.formatOffRouteSpeech(RESOURCES, notice)
        );
    }

    @Test
    public void formatWrongDirectionSpeech_isModeNeutral() {
        assertEquals("Wrong direction. Turn back", NavigationManeuverSpeechFormatter.formatWrongDirectionSpeech(
                RESOURCES
        ));
    }
}
