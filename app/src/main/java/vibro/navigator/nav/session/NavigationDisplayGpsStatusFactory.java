package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.presentation.NavStateResourceComposer;

final class NavigationDisplayGpsStatusFactory {
    private NavigationDisplayGpsStatusFactory() {
    }

    @NonNull
    static NavState withSnapshotGpsStatus(
            @NonNull NavState base,
            @NonNull NavigationDisplaySnapshot snapshot
    ) {
        return NavStateComposer.withGpsStatus(base, NavStateResourceComposer.buildGpsStatus(
                gpsStatusSpeedMps(snapshot),
                snapshot.lastFiltered,
                gpsStatusAccuracyMeters(snapshot),
                snapshot.fixedSatelliteCount,
                snapshot.acquiredFixCount,
                snapshot.nextEvaluationDeadlineElapsedMs,
                snapshot.textResources
        ));
    }

    private static float gpsStatusSpeedMps(@NonNull NavigationDisplaySnapshot snapshot) {
        return snapshot.lastFiltered == null ? Float.NaN : snapshot.displaySpeedMps;
    }

    private static float gpsStatusAccuracyMeters(@NonNull NavigationDisplaySnapshot snapshot) {
        return snapshot.lastFiltered == null ? Float.NaN : snapshot.accuracyMeters;
    }
}
