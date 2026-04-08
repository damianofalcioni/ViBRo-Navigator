package com.vibenavigator.nav;

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
    public final float visibleRadiusMeters;
    @NonNull
    public final List<RoutePoint> routePoints;
    public final float destinationEastMeters;
    public final float destinationNorthMeters;
    public final boolean destinationWithinRadius;

    public NavCompassState(
            float headingDegrees,
            float visibleRadiusMeters,
            @NonNull List<RoutePoint> routePoints,
            float destinationEastMeters,
            float destinationNorthMeters,
            boolean destinationWithinRadius
    ) {
        this.headingDegrees = headingDegrees;
        this.visibleRadiusMeters = visibleRadiusMeters;
        this.routePoints = Collections.unmodifiableList(routePoints);
        this.destinationEastMeters = destinationEastMeters;
        this.destinationNorthMeters = destinationNorthMeters;
        this.destinationWithinRadius = destinationWithinRadius;
    }
}
