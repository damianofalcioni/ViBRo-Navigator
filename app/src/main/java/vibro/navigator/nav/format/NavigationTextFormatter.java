package vibro.navigator.nav.format;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

import vibro.navigator.nav.directions.DirectionInfo;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.guidance.NavigationWrongDirectionNotice;
import vibro.navigator.nav.location.NavigationGpsTextFormatter;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;

public final class NavigationTextFormatter {

    private NavigationTextFormatter() {
    }

    @NonNull
    public static String formatTurnNotification(
            @NonNull Context context,
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        return formatTurnNotification(
                new AndroidNavigationTextResources(context),
                hint,
                distanceMeters,
                timeSeconds
        );
    }

    @NonNull
    public static String formatTurnNotification(
            @NonNull NavigationTextResources textResources,
            @NonNull VoiceHint hint,
            double distanceMeters,
            double timeSeconds
    ) {
        return NavigationTextFormatterRules.formatTurnNotification(
                textResources,
                hint,
                distanceMeters,
                timeSeconds
        );
    }

    @NonNull
    public static String formatDirectionSymbol(@NonNull DirectionInfo direction) {
        if (direction.exitNumber > 0) {
            return direction.emoji + direction.exitNumber;
        }
        return direction.emoji;
    }

    @NonNull
    public static String formatOffRouteNotification(
            @NonNull Context context,
            @NonNull NavigationRerouteNotice rerouteNotice
    ) {
        return NavigationTextFormatterRules.formatOffRouteNotification(
                new AndroidNavigationTextResources(context),
                rerouteNotice
        );
    }

    @NonNull
    public static String formatWrongDirectionNotification(
            @NonNull Context context,
            @NonNull NavigationWrongDirectionNotice wrongDirectionNotice
    ) {
        return NavigationTextFormatterRules.formatWrongDirectionNotification(
                new AndroidNavigationTextResources(context),
                wrongDirectionNotice
        );
    }

    @NonNull
    public static String formatStationaryOrientationNotification(
            @NonNull Context context,
            @NonNull StationaryOrientationAdvisor.Decision decision
    ) {
        return NavigationTextFormatterRules.formatStationaryOrientationNotification(
                new AndroidNavigationTextResources(context),
                decision
        );
    }

    @NonNull
    public static String formatDistance(@NonNull Context context, double meters) {
        return NavigationMeasurementFormatter.formatDistance(context, meters);
    }

    @NonNull
    public static String formatDistance(@NonNull NavigationTextResources resources, double meters) {
        return NavigationMeasurementFormatter.formatDistance(resources, meters);
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull Context context, int seconds) {
        return NavigationTextFormatterRules.formatTimeSeconds(
                new AndroidNavigationTextResources(context),
                seconds
        );
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull NavigationTextResources resources, int seconds) {
        return NavigationTextFormatterRules.formatTimeSeconds(resources, seconds);
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull Context context, double seconds) {
        return NavigationTextFormatterRules.formatTimeSeconds(
                new AndroidNavigationTextResources(context),
                seconds
        );
    }

    @NonNull
    public static String formatTimeSeconds(@NonNull NavigationTextResources resources, double seconds) {
        return NavigationTextFormatterRules.formatTimeSeconds(resources, seconds);
    }

    @NonNull
    public static String formatBearingDegrees(@NonNull Context context, @Nullable Double degrees) {
        return NavigationTextFormatterRules.formatBearingDegrees(
                new AndroidNavigationTextResources(context),
                degrees
        );
    }

    @NonNull
    public static String formatBearingDegrees(@NonNull NavigationTextResources resources, @Nullable Double degrees) {
        return NavigationTextFormatterRules.formatBearingDegrees(resources, degrees);
    }

    @NonNull
    public static String formatGpsStatus(
            @NonNull Context context,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTextFormatter.formatGpsStatus(
                context,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    @NonNull
    public static String formatGpsStatus(
            @NonNull NavigationTextResources resources,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTextFormatter.formatGpsStatus(
                resources,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        return NavigationGpsTextFormatter.formatSpeed(context, speedMps);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        return NavigationGpsTextFormatter.formatElevation(context, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        return NavigationGpsTextFormatter.formatAccuracy(context, accuracyMeters);
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        return NavigationGpsTextFormatter.formatFixedSatelliteCount(context, fixedSatelliteCount);
    }

    @NonNull
    public static String formatAcquiredFixCount(
            @NonNull Context context,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTextFormatter.formatAcquiredFixCount(context, acquiredFixCount);
    }

    @NonNull
    public static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        return NavigationGpsTextFormatter.formatGpsBearing(context, bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
            @NonNull Context context,
            @Nullable Float bearingAccuracyDegrees
    ) {
        return NavigationGpsTextFormatter.formatGpsBearingAccuracy(context, bearingAccuracyDegrees);
    }

    @NonNull
    public static String formatEta(long timeMs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMs);
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        );
    }
}
