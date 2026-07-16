package vibro.navigator.main;

import android.app.Activity;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppSettings;

final class MainActivityRoundTripRequestFactory {
    @NonNull
    private final Activity activity;
    @NonNull
    private final EditText distanceEdit;
    @NonNull
    private final EditText directionEdit;

    MainActivityRoundTripRequestFactory(
            @NonNull Activity activity,
            @NonNull EditText distanceEdit,
            @NonNull EditText directionEdit
    ) {
        this.activity = activity;
        this.distanceEdit = distanceEdit;
        this.directionEdit = directionEdit;
    }

    @Nullable
    NavigationRequest resolveRequest(@NonNull ProfileSelection profileSelection) {
        Integer radiusMeters = resolveBRouterRoundTripRadiusMeters();
        if (radiusMeters == null || !hasBRouterProfile(profileSelection)) {
            return null;
        }
        Integer directionDegrees = resolveBRouterRoundTripDirectionDegrees();
        return directionDegrees == null ? null : buildRequest(profileSelection, radiusMeters, directionDegrees);
    }

    @Nullable
    private Integer resolveBRouterRoundTripRadiusMeters() {
        Integer radiusMeters = RoundTripDistanceInput.parseBRouterRadiusMeters(
                distanceEdit.getText(),
                AppSettings.isImperialUnitsEnabled(activity)
        );
        if (radiusMeters == null) {
            Toast.makeText(activity, R.string.msg_invalid_round_trip_distance, Toast.LENGTH_SHORT).show();
        }
        return radiusMeters;
    }

    private boolean hasBRouterProfile(@NonNull ProfileSelection profileSelection) {
        if (profileSelection.routingMode == NavigationRoutingMode.BROUTER
                && profileSelection.profileName != null
                && !profileSelection.profileName.trim().isEmpty()) {
            return true;
        }
        Toast.makeText(activity, R.string.msg_round_trip_requires_brouter_profile, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Nullable
    private Integer resolveBRouterRoundTripDirectionDegrees() {
        Integer directionDegrees = RoundTripDirectionInput.parseDirectionDegrees(directionEdit.getText());
        if (directionDegrees == null) {
            Toast.makeText(activity, R.string.msg_invalid_round_trip_direction, Toast.LENGTH_SHORT).show();
        }
        return directionDegrees;
    }

    @NonNull
    private static NavigationRequest buildRequest(
            @NonNull ProfileSelection profileSelection,
            int radiusMeters,
            int directionDegrees
    ) {
        return new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                profileSelection.profileName,
                profileSelection.customProfile,
                profileSelection.profileParameters,
                null,
                null,
                Collections.emptyList(),
                radiusMeters,
                directionDegrees
        );
    }
}
