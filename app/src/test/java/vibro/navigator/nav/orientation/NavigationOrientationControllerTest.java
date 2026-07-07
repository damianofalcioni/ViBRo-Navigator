package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

@RunWith(RobolectricTestRunner.class)
public class NavigationOrientationControllerTest {

    private static final long START_MS = 1_000L;
    private static final long AFTER_DWELL_MS = 6_500L;

    @Test
    public void shouldDispatchCompassUiRequiresActiveRouteVisibleUiAndInteractiveScreen() {
        assertTrue(NavigationOrientationController.shouldDispatchCompassUi(true, true, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(false, true, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(true, false, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(true, true, false));
    }

    @Test
    public void shouldEvaluateStationaryOrientationRequiresActiveRouteAndNoReroute() {
        assertTrue(NavigationOrientationController.shouldEvaluateStationaryOrientation(true, false));
        assertFalse(NavigationOrientationController.shouldEvaluateStationaryOrientation(false, false));
        assertFalse(NavigationOrientationController.shouldEvaluateStationaryOrientation(true, true));
    }

    @Test
    public void headingSampleUpdateSendsStationaryOrientationAfterLocationEvaluationWaitedForSensor() {
        Context context = ApplicationProvider.getApplicationContext();
        MutableClock clock = new MutableClock(START_MS);
        FakeHeadingMonitor headingMonitor = new FakeHeadingMonitor();
        RecordingForegroundController foregroundController = new RecordingForegroundController();
        NavigationOrientationController controller = new NavigationOrientationController(
                callback -> headingMonitor.attach(callback),
                () -> 0,
                clock,
                Runnable::run,
                new PassiveCompassUiState()
        );
        NavigationSession session = straightLineSession(context);
        controller.start();

        controller.maybeSendStationaryOrientationNotification(session, foregroundController);
        clock.nowMs = AFTER_DWELL_MS;
        headingMonitor.emit(sample(270.0, AFTER_DWELL_MS));

        assertEquals(1, foregroundController.decisions.size());
        assertEquals(180.0, foregroundController.decisions.get(0).absoluteTurnDegrees(), 0.001);
    }

    @NonNull
    private static NavigationSession straightLineSession(@NonNull Context context) {
        NavigationSession session = new NavigationSession();
        session.loadRequest(new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE,
                null,
                "Destination",
                new LatLon(0.0, 0.001),
                Collections.emptyList()
        ));
        assertTrue(session.start(context, START_MS));
        session.onRawLocationChanged(context, location(START_MS), START_MS);
        return session;
    }

    @NonNull
    private static NavigationLocation location(long timeMs) {
        NavigationLocation location = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }

    @NonNull
    private static GeomagneticOrientationMonitor.Sample sample(double headingDegrees, long elapsedRealtimeMs) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                HeadingAccuracyStatus.HIGH,
                3.0,
                elapsedRealtimeMs
        );
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs;

        MutableClock(long nowMs) {
            this.nowMs = nowMs;
        }

        @Override
        public long elapsedRealtimeMs() {
            return nowMs;
        }
    }

    private static final class FakeHeadingMonitor implements NavigationHeadingMonitor {
        @Nullable
        private GeomagneticOrientationMonitor.Callback callback;
        @Nullable
        private GeomagneticOrientationMonitor.Sample latestSample;

        @NonNull
        FakeHeadingMonitor attach(@NonNull GeomagneticOrientationMonitor.Callback callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public void stop() {
        }

        @Nullable
        @Override
        public GeomagneticOrientationMonitor.Sample getLatestSample() {
            return latestSample;
        }

        void emit(@NonNull GeomagneticOrientationMonitor.Sample sample) {
            latestSample = sample;
            if (callback != null) {
                callback.onSampleUpdated(sample);
            }
        }
    }

    private static final class PassiveCompassUiState implements NavigationOrientationController.CompassUiState {
        @Override
        public boolean shouldDispatchCompassUi() {
            return false;
        }

        @Override
        public boolean hasStateListeners() {
            return false;
        }

        @Override
        public void requestStateRefresh() {
        }
    }

    private static final class RecordingForegroundController implements NavigationForegroundController {
        private final List<StationaryOrientationAdvisor.Decision> decisions = new ArrayList<>();

        @Override
        public void ensureChannels() {
        }

        @Override
        public void promoteToForeground(@NonNull NavigationRequest request, boolean paused) {
        }

        @Override
        public void stopForegroundService() {
        }

        @Override
        public boolean isOngoingNotificationVisible() {
            return false;
        }

        @Override
        public void sendImminentTurnNotification(
                @NonNull VoiceHint hint,
                double distanceMeters,
                double timeSeconds
        ) {
        }

        @Override
        public void sendStationaryOrientationNotification(
                @NonNull StationaryOrientationAdvisor.Decision decision
        ) {
            decisions.add(decision);
        }

        @Override
        public void sendOffRouteNotification(@NonNull NavigationRerouteNotice rerouteNotice) {
        }

        @Override
        public void sendWrongDirectionNotification(@NonNull NavigationWrongDirectionNotice wrongDirectionNotice) {
        }
    }

}
