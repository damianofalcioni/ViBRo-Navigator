package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;

final class NavigationRouteGpxXmlWriter {
    static final String TAG_NAME = "name";
    static final String TAG_DESC = "desc";
    static final String TAG_TYPE = "type";
    static final String LINE_END = "\n";

    private static final String GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1";
    private static final String CREATOR = "ViBRo Navigator";

    private NavigationRouteGpxXmlWriter() {
    }

    static void appendHeader(@NonNull StringBuilder out) {
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(LINE_END);
        out.append("<gpx version=\"1.1\" creator=\"")
                .append(escapeXml(CREATOR))
                .append("\" xmlns=\"")
                .append(GPX_NAMESPACE)
                .append("\">")
                .append(LINE_END);
    }

    static void appendFooter(@NonNull StringBuilder out) {
        out.append("</gpx>").append(LINE_END);
    }

    static void appendMetadata(@NonNull StringBuilder out, @NonNull String routeName) {
        out.append("  <metadata>").append(LINE_END);
        appendSimpleElement(out, 2, TAG_NAME, routeName);
        out.append("  </metadata>").append(LINE_END);
    }

    static void appendRoute(
            @NonNull StringBuilder out,
            @NonNull String routeName,
            @NonNull GeoJsonRoute route
    ) {
        out.append("  <rte>").append(LINE_END);
        appendSimpleElement(out, 2, TAG_NAME, routeName);
        appendPointList(out, route, "rtept", 2);
        out.append("  </rte>").append(LINE_END);
    }

    static void appendTrack(
            @NonNull StringBuilder out,
            @NonNull String routeName,
            @NonNull GeoJsonRoute route
    ) {
        out.append("  <trk>").append(LINE_END);
        appendSimpleElement(out, 2, TAG_NAME, routeName);
        out.append("    <trkseg>").append(LINE_END);
        appendPointList(out, route, "trkpt", 3);
        out.append("    </trkseg>").append(LINE_END);
        out.append("  </trk>").append(LINE_END);
    }

    static void appendPointStart(
            @NonNull StringBuilder out,
            int indentLevel,
            @NonNull String tag,
            @NonNull LatLon point
    ) {
        appendIndent(out, indentLevel);
        out.append("<")
                .append(tag)
                .append(" lat=\"")
                .append(formatCoordinate(point.lat))
                .append("\" lon=\"")
                .append(formatCoordinate(point.lon))
                .append("\"");
    }

    static void appendSimpleElement(
            @NonNull StringBuilder out,
            int indentLevel,
            @NonNull String tag,
            @NonNull String value
    ) {
        appendIndent(out, indentLevel);
        out.append("<")
                .append(tag)
                .append(">")
                .append(escapeXml(value))
                .append("</")
                .append(tag)
                .append(">")
                .append(LINE_END);
    }

    private static void appendPointList(
            @NonNull StringBuilder out,
            @NonNull GeoJsonRoute route,
            @NonNull String tag,
            int indentLevel
    ) {
        for (LatLon point : route.track) {
            appendPointStart(out, indentLevel, tag, point);
            out.append(" />").append(LINE_END);
        }
    }

    private static void appendIndent(@NonNull StringBuilder out, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            out.append("  ");
        }
    }

    @NonNull
    private static String formatCoordinate(double coordinate) {
        return String.format(Locale.US, "%.6f", coordinate);
    }

    @NonNull
    private static String escapeXml(@NonNull String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            appendEscapedXmlChar(out, value.charAt(i));
        }
        return out.toString();
    }

    private static void appendEscapedXmlChar(@NonNull StringBuilder out, char value) {
        switch (value) {
            case '&':
                out.append("&amp;");
                break;
            case '<':
                out.append("&lt;");
                break;
            case '>':
                out.append("&gt;");
                break;
            case '"':
                out.append("&quot;");
                break;
            case '\'':
                out.append("&apos;");
                break;
            default:
                out.append(value);
                break;
        }
    }
}
