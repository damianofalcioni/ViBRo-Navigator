package vibro.navigator.nav.export;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;

final class NavigationRouteGpxStopWriter {
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TYPE_INTERMEDIATE_STOP = "vibro.navigator.stop";

    private NavigationRouteGpxStopWriter() {
    }

    static void appendWaypoints(
            @NonNull StringBuilder out,
            @NonNull Context context,
            @NonNull List<LatLon> intermediateStops
    ) {
        for (int i = 0; i < intermediateStops.size(); i++) {
            appendWaypoint(out, context, intermediateStops.get(i), i);
        }
    }

    private static void appendWaypoint(
            @NonNull StringBuilder out,
            @NonNull Context context,
            @NonNull LatLon stop,
            int index
    ) {
        NavigationRouteGpxXmlWriter.appendPointStart(out, 1, TAG_WAYPOINT, stop);
        out.append(">").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_NAME,
                context.getString(R.string.format_stop_label, index + 1)
        );
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_TYPE,
                TYPE_INTERMEDIATE_STOP
        );
        out.append("  </wpt>").append(NavigationRouteGpxXmlWriter.LINE_END);
    }
}
