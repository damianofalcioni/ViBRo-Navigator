package vibro.navigator.nav.export;

import vibro.navigator.nav.location.NavigationLocation;

/** Provider uncertainty belongs to the source measurement; exported coordinates are filtered. */
final class NavigationRouteGpxFixDiagnostics {
    private NavigationRouteGpxFixDiagnostics() {
    }

    static void append(StringBuilder out, NavigationLocation location) {
        out.append("    <extensions>\n      <fix xmlns=\"urn:vibro:navigator:gpx:1\">\n");
        if (location.getProvider() != null) {
            NavigationRouteGpxXmlWriter.appendSimpleElement(out, 4, "provider", location.getProvider());
        }
        appendMeasurement(out, "accuracyMeters", location.hasAccuracy(), location.getAccuracy());
        appendMeasurement(out, "speedMps", location.hasSpeed(), location.getSpeed());
        appendMeasurement(out, "bearingDegrees", location.hasBearing(), location.getBearing());
        appendMeasurement(out, "bearingAccuracyDegrees", location.hasBearingAccuracy(),
                location.getBearingAccuracyDegrees());
        out.append("      </fix>\n    </extensions>\n");
    }

    private static void appendMeasurement(StringBuilder out, String tag, boolean available, float value) {
        if (available && Float.isFinite(value) && value >= 0) {
            NavigationRouteGpxXmlWriter.appendSimpleElement(out, 4, tag, Float.toString(value));
        }
    }
}
