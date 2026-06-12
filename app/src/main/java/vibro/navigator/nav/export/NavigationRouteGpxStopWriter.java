package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;

final class NavigationRouteGpxStopWriter {
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TYPE_INTERMEDIATE_STOP = "vibro.navigator.stop";
    private static final String TYPE_DESTINATION = "vibro.navigator.destination";

    private NavigationRouteGpxStopWriter() {
    }

    static void appendWaypoints(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull List<LatLon> intermediateStops
    ) {
        for (int i = 0; i < intermediateStops.size(); i++) {
            appendWaypoint(out, textResources, intermediateStops.get(i), i);
        }
    }

    static void appendDestinationWaypoint(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull LatLon destination
    ) {
        appendWaypoint(
                out,
                destination,
                textResources.getString(R.string.nav_destination_label),
                TYPE_DESTINATION
        );
    }

    private static void appendWaypoint(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull LatLon stop,
            int index
    ) {
        appendWaypoint(
                out,
                stop,
                textResources.getString(R.string.format_stop_label, index + 1),
                TYPE_INTERMEDIATE_STOP
        );
    }

    private static void appendWaypoint(
            @NonNull StringBuilder out,
            @NonNull LatLon point,
            @NonNull String name,
            @NonNull String type
    ) {
        NavigationRouteGpxXmlWriter.appendPointStart(out, 1, TAG_WAYPOINT, point);
        out.append(">").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_NAME,
                name
        );
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_TYPE,
                type
        );
        out.append("  </wpt>").append(NavigationRouteGpxXmlWriter.LINE_END);
    }
}
