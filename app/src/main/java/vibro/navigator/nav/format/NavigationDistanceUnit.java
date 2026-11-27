package vibro.navigator.nav.format;

import vibro.navigator.R;

/** Display unit selection shared by formatted guidance and host-native distances. */
public enum NavigationDistanceUnit {
    METERS(1.0, R.string.format_distance_m),
    KILOMETERS(1_000.0, R.string.format_distance_km),
    FEET(0.3048, R.string.format_distance_ft),
    MILES(1_609.344, R.string.format_distance_mi);

    private final double metersPerUnit;
    final int formatResource;

    NavigationDistanceUnit(double metersPerUnit, int formatResource) {
        this.metersPerUnit = metersPerUnit;
        this.formatResource = formatResource;
    }

    public static NavigationDistanceUnit select(double meters, boolean imperial) {
        if (imperial) {
            return MILES.fromMeters(meters) >= 0.1 ? MILES : FEET;
        }
        return meters >= KILOMETERS.metersPerUnit ? KILOMETERS : METERS;
    }

    public double fromMeters(double meters) {
        return meters / metersPerUnit;
    }
}
