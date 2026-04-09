package com.vibenavigator.nav;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public final class NavCompassState {

    public static final class RoutePoint {
        public final float eastMeters;
        public final float northMeters;

        public RoutePoint(float eastMeters, float northMeters) {
            this.eastMeters = eastMeters;
            this.northMeters = northMeters;
        }
    }

    public final float headingDegrees;
    @Nullable
    public final Float headingAccuracyDegrees;
    public final float referenceSpeedMps;
    public final float visibleRadiusMeters;
    public final float accuracyRadiusMeters;
    @NonNull
    public final List<RoutePoint> passedRoutePoints;
    @NonNull
    public final List<RoutePoint> routePoints;
    @NonNull
    public final List<RoutePoint> hintPoints;
    public final float destinationEastMeters;
    public final float destinationNorthMeters;
    public final boolean destinationWithinRadius;

    public NavCompassState(
            float headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            float referenceSpeedMps,
            float visibleRadiusMeters,
            float accuracyRadiusMeters,
            @NonNull List<RoutePoint> passedRoutePoints,
            @NonNull List<RoutePoint> routePoints,
            @NonNull List<RoutePoint> hintPoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.headingDegrees = headingDegrees;
        this.headingAccuracyDegrees = headingAccuracyDegrees;
        this.referenceSpeedMps = referenceSpeedMps;
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.accuracyRadiusMeters = accuracyRadiusMeters;
        this.passedRoutePoints = Collections.unmodifiableList(passedRoutePoints);
        this.routePoints = Collections.unmodifiableList(routePoints);
        this.hintPoints = Collections.unmodifiableList(hintPoints);
        this.destinationEastMeters = destinationEastMeters;
        this.destinationNorthMeters = destinationNorthMeters;
        this.destinationWithinRadius = destinationWithinRadius;
    }
}
