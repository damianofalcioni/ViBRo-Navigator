package vibro.navigator.nav;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationSessionHeadingResolver {

    private final NavigationSessionLocationState locationState;

    NavigationSessionHeadingResolver(@NonNull NavigationSessionLocationState locationState) {
        this.locationState = locationState;
    }

    @NonNull
    Selection selectHeading(
            @Nullable Location lastFiltered,
            boolean likelyStationary,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees
    ) {
        Selection compassHeading = selectCompassHeading(lastFiltered, likelyStationary);
        if (compassHeading.hasHeading()) {
            return compassHeading;
        }
        if (displayHeadingDegrees != null) {
            return new Selection(displayHeadingDegrees, displayHeadingAccuracyDegrees);
        }
        return new Selection(actualBearingDegrees(lastFiltered), null);
    }

    @NonNull
    private Selection selectCompassHeading(@Nullable Location lastFiltered, boolean likelyStationary) {
        if (lastFiltered == null) {
            return Selection.none();
        }
        NavigationSessionLocationState.HeadingEstimate preferredCompassHeading =
                locationState.preferredCompassHeading(lastFiltered, likelyStationary);
        if (preferredCompassHeading == null) {
            return Selection.none();
        }
        return new Selection(
                preferredCompassHeading.headingDegrees,
                preferredCompassHeading.headingAccuracyDegrees
        );
    }

    @Nullable
    private Double actualBearingDegrees(@Nullable Location lastFiltered) {
        return lastFiltered != null ? locationState.actualBearingDegrees(lastFiltered) : null;
    }

    static final class Selection {
        @Nullable
        final Double headingDegrees;
        @Nullable
        final Float headingAccuracyDegrees;

        private Selection(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
        }

        @NonNull
        static Selection none() {
            return new Selection(null, null);
        }

        boolean hasHeading() {
            return headingDegrees != null;
        }
    }
}
