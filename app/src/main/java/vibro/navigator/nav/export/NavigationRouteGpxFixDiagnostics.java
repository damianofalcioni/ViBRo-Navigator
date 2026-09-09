package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocation;

/** Provider uncertainty belongs to the source measurement; exported coordinates are filtered. */
final class NavigationRouteGpxFixDiagnostics {
    private static final String FIX_NAMESPACE = "urn:vibro:navigator:gpx:1";
    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_ACCURACY_METERS = "accuracyMeters";
    private static final String TAG_SPEED_MPS = "speedMps";
    private static final String TAG_BEARING_DEGREES = "bearingDegrees";
    private static final String TAG_BEARING_ACCURACY_DEGREES = "bearingAccuracyDegrees";
    private static final int WAYPOINT_CHILD_INDENT = 2;

    private NavigationRouteGpxFixDiagnostics() {
    }

    static void append(@NonNull StringBuilder out, @NonNull NavigationLocation location) {
        append(out, location, WAYPOINT_CHILD_INDENT);
    }

    static void append(@NonNull StringBuilder out, @NonNull NavigationLocation location, int indentLevel) {
        NavigationRouteGpxXmlWriter.appendIndent(out, indentLevel);
        out.append("<extensions>").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendIndent(out, indentLevel + 1);
        out.append("<fix xmlns=\"").append(FIX_NAMESPACE).append("\">")
                .append(NavigationRouteGpxXmlWriter.LINE_END);
        if (location.getProvider() != null) {
            NavigationRouteGpxXmlWriter.appendSimpleElement(
                    out,
                    indentLevel + 2,
                    TAG_PROVIDER,
                    location.getProvider()
            );
        }
        appendMeasurement(out, indentLevel, TAG_ACCURACY_METERS, location.hasAccuracy(), location.getAccuracy());
        appendMeasurement(out, indentLevel, TAG_SPEED_MPS, location.hasSpeed(), location.getSpeed());
        appendMeasurement(out, indentLevel, TAG_BEARING_DEGREES, location.hasBearing(), location.getBearing());
        appendMeasurement(out, indentLevel, TAG_BEARING_ACCURACY_DEGREES, location.hasBearingAccuracy(),
                location.getBearingAccuracyDegrees());
        NavigationRouteGpxXmlWriter.appendIndent(out, indentLevel + 1);
        out.append("</fix>").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendIndent(out, indentLevel);
        out.append("</extensions>").append(NavigationRouteGpxXmlWriter.LINE_END);
    }

    private static void appendMeasurement(
            @NonNull StringBuilder out,
            int baseIndentLevel,
            @NonNull String tag,
            boolean available,
            float value
    ) {
        if (available && Float.isFinite(value) && value >= 0) {
            NavigationRouteGpxXmlWriter.appendSimpleElement(
                    out,
                    baseIndentLevel + 2,
                    tag,
                    Float.toString(value)
            );
        }
    }
}
