package vibro.navigator.intent;

import androidx.annotation.NonNull;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;

/** Parses GPX waypoint, route, or track points into the main route form. */
public final class GpxWaypointParser {
    private static final String EMPTY_EXTERNAL_ENTITY = "";
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TAG_ROUTE_POINT = "rtept";
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_NAME = "name";
    private static final String TAG_TYPE = "type";
    private static final String TYPE_INTERMEDIATE_STOP = "vibro.navigator.stop";
    private static final String TYPE_TURN_INSTRUCTION = "vibro.navigator.turn";
    private static final String TYPE_GPS_FIX = "vibro.navigator.gps-fix";
    private static final byte[] DOCTYPE_PATTERN =
            "<!DOCTYPE".getBytes(StandardCharsets.US_ASCII);
    private static final String FEATURE_DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    @NonNull
    public GpxWaypointRoute parse(@NonNull InputStream input) throws IOException {
        try {
            WaypointHandler handler = new WaypointHandler();
            SAXParser parser = newSecureParser();
            parser.getXMLReader().setEntityResolver(blockExternalEntities());
            parser.parse(new DoctypeRejectingInputStream(input), handler);
            return routeFrom(handler);
        } catch (ParserConfigurationException | SAXException | IllegalArgumentException e) {
            throw new IOException("Invalid GPX waypoint document", e);
        }
    }

    @NonNull
    private static GpxWaypointRoute routeFrom(@NonNull WaypointHandler handler) {
        List<Poi> waypoints = routeWaypoints(handler.waypoints(), handler.geometryDestination());
        if (!waypoints.isEmpty()) {
            return GpxWaypointRoute.fromWaypoints(waypoints);
        }
        if (!handler.routePoints().isEmpty()) {
            return GpxWaypointRoute.fromWaypoints(handler.routePoints());
        }
        return GpxWaypointRoute.fromTrackPoints(handler.trackPoints());
    }

    @NonNull
    private static List<Poi> routeWaypoints(
            @NonNull List<GpxWaypoint> waypoints,
            @NonNull Poi geometryDestination
    ) {
        List<Poi> routePoints = new ArrayList<>();
        boolean hasDestinationLikeWaypoint = false;
        for (GpxWaypoint waypoint : waypoints) {
            if (isIgnoredWaypointType(waypoint.type)) {
                continue;
            }
            routePoints.add(waypoint.poi);
            if (!TYPE_INTERMEDIATE_STOP.equals(waypoint.type)) {
                hasDestinationLikeWaypoint = true;
            }
        }
        if (!routePoints.isEmpty()
                && !hasDestinationLikeWaypoint
                && geometryDestination.hasValidCoordinates()) {
            routePoints.add(geometryDestination);
        }
        return routePoints;
    }

    private static boolean isIgnoredWaypointType(@NonNull String type) {
        return TYPE_TURN_INSTRUCTION.equals(type) || TYPE_GPS_FIX.equals(type);
    }

    @NonNull
    private static SAXParser newSecureParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        setFeatureIfSupported(factory, FEATURE_DISALLOW_DOCTYPE, true);
        setFeatureIfSupported(factory, FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
        setFeatureIfSupported(factory, FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
        setFeatureIfSupported(factory, FEATURE_LOAD_EXTERNAL_DTD, false);
        return factory.newSAXParser();
    }

    private static void setFeatureIfSupported(
            @NonNull SAXParserFactory factory,
            @NonNull String feature,
            boolean value
    ) throws ParserConfigurationException {
        try {
            factory.setFeature(feature, value);
        } catch (SAXException ignored) {
            // Older Android parsers may not recognize all hardening flags.
        }
    }

    @NonNull
    private static EntityResolver blockExternalEntities() {
        return (publicId, systemId) -> new InputSource(new StringReader(EMPTY_EXTERNAL_ENTITY));
    }

    private static final class DoctypeRejectingInputStream extends FilterInputStream {
        private int matchIndex;

        private DoctypeRejectingInputStream(@NonNull InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                rejectDoctypeByte(value);
            }
            return value;
        }

        @Override
        public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count <= 0) {
                return count;
            }
            int end = offset + count;
            for (int index = offset; index < end; index++) {
                rejectDoctypeByte(buffer[index]);
            }
            return count;
        }

        private void rejectDoctypeByte(int value) throws IOException {
            int normalized = asciiUpper(value);
            if (normalized == DOCTYPE_PATTERN[matchIndex]) {
                matchIndex++;
                if (matchIndex == DOCTYPE_PATTERN.length) {
                    throw new IOException("GPX document type declarations are not supported");
                }
                return;
            }
            matchIndex = normalized == DOCTYPE_PATTERN[0] ? 1 : 0;
        }

        private static int asciiUpper(int value) {
            int unsigned = value & 0xff;
            if (unsigned >= 'a' && unsigned <= 'z') {
                return unsigned - ('a' - 'A');
            }
            return unsigned;
        }
    }

    private static final class WaypointHandler extends DefaultHandler {
        @NonNull
        private final List<GpxWaypoint> waypoints = new ArrayList<>();
        @NonNull
        private final List<Poi> routePoints = new ArrayList<>();
        @NonNull
        private final List<Poi> trackPoints = new ArrayList<>();
        private int elementDepth;
        private int pointDepth = -1;
        private int nameDepth = -1;
        private int typeDepth = -1;
        private double latitude = Double.NaN;
        private double longitude = Double.NaN;
        @NonNull
        private PointKind pointKind = PointKind.NONE;
        @NonNull
        private StringBuilder name = new StringBuilder();
        @NonNull
        private StringBuilder type = new StringBuilder();

        @Override
        public void startElement(
                String uri,
                String localName,
                String qualifiedName,
                Attributes attributes
        ) {
            elementDepth++;
            if (pointKind == PointKind.NONE) {
                startPointIfNeeded(localName, qualifiedName, attributes);
                return;
            }
            if (elementDepth == pointDepth + 1 && isTag(localName, qualifiedName, TAG_NAME)) {
                nameDepth = elementDepth;
            }
            if (elementDepth == pointDepth + 1 && isTag(localName, qualifiedName, TAG_TYPE)) {
                typeDepth = elementDepth;
            }
        }

        @Override
        public void characters(char[] chars, int start, int length) {
            if (nameDepth >= 0) {
                name.append(chars, start, length);
            }
            if (typeDepth >= 0) {
                type.append(chars, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qualifiedName) {
            if (nameDepth == elementDepth && isTag(localName, qualifiedName, TAG_NAME)) {
                nameDepth = -1;
            }
            if (typeDepth == elementDepth && isTag(localName, qualifiedName, TAG_TYPE)) {
                typeDepth = -1;
            }
            if (pointDepth == elementDepth && pointKind.matches(localName, qualifiedName)) {
                addCurrentWaypoint();
                pointDepth = -1;
                nameDepth = -1;
                typeDepth = -1;
                pointKind = PointKind.NONE;
            }
            elementDepth--;
        }

        @NonNull
        List<GpxWaypoint> waypoints() {
            return waypoints;
        }

        @NonNull
        List<Poi> routePoints() {
            return routePoints;
        }

        @NonNull
        List<Poi> trackPoints() {
            return trackPoints;
        }

        @NonNull
        Poi geometryDestination() {
            if (!routePoints.isEmpty()) {
                return routePoints.get(routePoints.size() - 1);
            }
            if (!trackPoints.isEmpty()) {
                return trackPoints.get(trackPoints.size() - 1);
            }
            return new Poi("", Double.NaN, Double.NaN);
        }

        private void startPointIfNeeded(
                @NonNull String localName,
                @NonNull String qualifiedName,
                @NonNull Attributes attributes
        ) {
            PointKind kind = PointKind.fromTag(localName, qualifiedName);
            if (kind == PointKind.NONE) {
                return;
            }
            pointKind = kind;
            pointDepth = elementDepth;
            latitude = coordinate(attributes.getValue("lat"));
            longitude = coordinate(attributes.getValue("lon"));
            name = new StringBuilder();
            type = new StringBuilder();
        }

        private void addCurrentWaypoint() {
            if (!LatLon.isValidCoordinate(latitude, longitude)) {
                return;
            }
            Poi poi = new Poi(name.toString().trim(), latitude, longitude);
            if (pointKind == PointKind.WAYPOINT) {
                waypoints.add(new GpxWaypoint(poi, type.toString().trim()));
            } else if (pointKind == PointKind.ROUTE_POINT) {
                routePoints.add(poi);
            } else if (pointKind == PointKind.TRACK_POINT) {
                trackPoints.add(poi);
            }
        }

        private static boolean isTag(
                @NonNull String localName,
                @NonNull String qualifiedName,
                @NonNull String expected
        ) {
            return expected.equals(localName)
                    || expected.equals(qualifiedName)
                    || qualifiedName.endsWith(":" + expected);
        }

        private static double coordinate(String value) {
            if (value == null) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }
    }

    private enum PointKind {
        NONE(""),
        WAYPOINT(TAG_WAYPOINT),
        ROUTE_POINT(TAG_ROUTE_POINT),
        TRACK_POINT(TAG_TRACK_POINT);

        @NonNull
        private final String tag;

        PointKind(@NonNull String tag) {
            this.tag = tag;
        }

        @NonNull
        static PointKind fromTag(@NonNull String localName, @NonNull String qualifiedName) {
            for (PointKind kind : values()) {
                if (kind != NONE && kind.matches(localName, qualifiedName)) {
                    return kind;
                }
            }
            return NONE;
        }

        boolean matches(@NonNull String localName, @NonNull String qualifiedName) {
            return tag.equals(localName)
                    || tag.equals(qualifiedName)
                    || qualifiedName.endsWith(":" + tag);
        }
    }

    private static final class GpxWaypoint {
        @NonNull
        private final Poi poi;
        @NonNull
        private final String type;

        private GpxWaypoint(@NonNull Poi poi, @NonNull String type) {
            this.poi = poi;
            this.type = type;
        }
    }
}
