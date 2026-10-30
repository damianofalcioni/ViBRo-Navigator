package vibro.navigator.nav.routing;

import org.junit.Test;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import static org.junit.Assert.*;

public class NavigationRouteTimingTest {
    @Test
    public void readyCandidateSeparatesCalculationDeliveryConfirmationAndApplication() {
        Clock clock = new Clock();
        NavigationRouteTiming timing = new NavigationRouteTiming();
        timing.start(clock);
        clock.nowMs += 700;
        assertEquals("calculationMs=700", timing.calculationCompleted());
        clock.nowMs += 20;
        timing.resultReceived();
        clock.nowMs += 6_300;
        timing.confirmed();
        clock.nowMs += 50;
        timing.applicationStarted();
        clock.nowMs += 40;
        assertEquals("calculationMs=700 callbackWaitMs=20 confirmationWaitMs=6300 applicationMs=40 totalMs=7110",
                timing.applicationCompleted());
    }

    @Test
    public void confirmationBeforeResultHasNoConfirmationWaitAndRepeatedConfirmationDoesNotRenewIt() {
        Clock clock = new Clock();
        NavigationRouteTiming timing = new NavigationRouteTiming();
        timing.start(clock);
        clock.nowMs += 500;
        timing.confirmed();
        clock.nowMs += 200;
        timing.calculationCompleted();
        clock.nowMs += 20;
        timing.resultReceived();
        clock.nowMs += 30;
        timing.confirmed();
        timing.applicationStarted();
        clock.nowMs += 40;
        assertEquals("calculationMs=700 callbackWaitMs=20 confirmationWaitMs=0 applicationMs=40 totalMs=790",
                timing.applicationCompleted());
    }

    @Test
    public void untrackedDomainApplicationsDoNotInventObservedDurations() {
        NavigationRouteTiming timing = new NavigationRouteTiming();
        timing.applicationStarted();
        assertEquals("calculationMs=unobserved callbackWaitMs=unobserved confirmationWaitMs=unobserved"
                + " applicationMs=unobserved totalMs=unobserved", timing.applicationCompleted());
    }

    private static final class Clock implements ElapsedRealtimeClock {
        private long nowMs = 5_000_000;
        @Override public long elapsedRealtimeMs() { return nowMs; }
        @Override public long uptimeMs() { return nowMs - 1_300_000; }
    }
}
