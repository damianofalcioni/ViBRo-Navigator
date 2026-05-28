package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationSessionHeadingResolver {

    private final NavigationSessionLocationState locationState;

    public NavigationSessionHeadingResolver(@NonNull NavigationSessionLocationState locationState) {
        this.locationState = locationState;
    }

    @NonNull
    public Selection selectHeading(
            @Nullable NavigationLocation lastFiltered,
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
    private Selection selectCompassHeading(@Nullable NavigationLocation lastFiltered, boolean likelyStationary) {
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
    private Double actualBearingDegrees(@Nullable NavigationLocation lastFiltered) {
        return lastFiltered != null ? locationState.actualBearingDegrees(lastFiltered) : null;
    }

    public static final class Selection {
        @Nullable
        public final Double headingDegrees;
        @Nullable
        public final Float headingAccuracyDegrees;

        private Selection(@Nullable Double headingDegrees, @Nullable Float headingAccuracyDegrees) {
            this.headingDegrees = headingDegrees;
            this.headingAccuracyDegrees = headingAccuracyDegrees;
        }

        @NonNull
        public static Selection none() {
            return new Selection(null, null);
        }

        public boolean hasHeading() {
            return headingDegrees != null;
        }
    }
}
