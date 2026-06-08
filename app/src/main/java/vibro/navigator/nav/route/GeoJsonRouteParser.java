package vibro.navigator.nav.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.LatLon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GeoJsonRouteParser {
    private static final String DEFAULT_ROUTE_METRIC = "0";

    private GeoJsonRouteParser() {
    }

    @NonNull
    public static GeoJsonRoute parse(@NonNull String geoJson) {
        try {
            JSONObject root = new JSONObject(geoJson);
            JSONArray features = root.optJSONArray("features");
            if (features == null || features.length() == 0) {
                return emptyRoute();
            }

            JSONObject trackFeature = GeoJsonRouteFeatureSelector.firstTrackFeature(features);
            if (trackFeature == null) {
                return emptyRoute();
            }

            return parseRoute(trackFeature);
        } catch (JSONException ignored) {
            return emptyRoute();
        }
    }

    @NonNull
    private static GeoJsonRoute emptyRoute() {
        return new GeoJsonRoute(new ArrayList<>(), new ArrayList<>(), 0, 0);
    }

    @NonNull
    private static GeoJsonRoute parseRoute(@NonNull JSONObject trackFeature) {
        JSONObject props = trackFeature.optJSONObject("properties");
        List<LatLon> track = parseTrack(trackFeature.optJSONObject("geometry"));
        if (track == null) {
            return emptyRoute();
        }
        return new GeoJsonRoute(
                track,
                parseVoiceHints(props, track.size()),
                GeoJsonRouteTimesParser.parse(props != null ? props.optJSONArray("times") : null),
                GeoJsonRouteSpeedLimitParser.parse(props != null ? props.optJSONArray("messages") : null),
                parseRouteMetricDouble(routeMetric(props, "total-time")),
                parseRouteMetricDouble(routeMetric(props, "track-length"))
        );
    }

    @NonNull
    private static String routeMetric(JSONObject props, @NonNull String key) {
        if (props == null) {
            return DEFAULT_ROUTE_METRIC;
        }
        return props.optString(key, DEFAULT_ROUTE_METRIC);
    }

    @NonNull
    private static List<VoiceHint> parseVoiceHints(JSONObject props, int trackSize) {
        List<VoiceHint> voiceHints = new ArrayList<>();
        JSONArray rawHints = props != null ? props.optJSONArray("voicehints") : null;
        if (rawHints == null || trackSize <= 0) {
            return voiceHints;
        }
        for (int i = 0; i < rawHints.length(); i++) {
            VoiceHint voiceHint = parseVoiceHint(rawHints.optJSONArray(i), trackSize);
            if (voiceHint != null) {
                voiceHints.add(voiceHint);
            }
        }
        return voiceHints;
    }

    private static VoiceHint parseVoiceHint(JSONArray hint, int trackSize) {
        if (hint == null || hint.length() < 5) {
            return null;
        }
        int indexInTrack = hint.optInt(0, -1);
        double distanceToNextMeters = hint.optDouble(3, Double.NaN);
        if (indexInTrack < 0
                || indexInTrack >= trackSize
                || !Double.isFinite(distanceToNextMeters)
                || distanceToNextMeters < 0.0) {
            return null;
        }
        return new VoiceHint(
                indexInTrack,
                hint.optInt(1, 0),
                hint.optInt(2, 0),
                distanceToNextMeters,
                hint.optInt(4, 0)
        );
    }

    @Nullable
    private static List<LatLon> parseTrack(JSONObject geom) {
        List<LatLon> track = new ArrayList<>();
        JSONArray coords = geom != null ? geom.optJSONArray("coordinates") : null;
        if (coords == null) {
            return track;
        }
        for (int i = 0; i < coords.length(); i++) {
            LatLon point = parseTrackPoint(coords.optJSONArray(i));
            if (point == null) {
                return null;
            }
            track.add(point);
        }
        return track;
    }

    private static LatLon parseTrackPoint(JSONArray point) {
        if (point == null || point.length() < 2) {
            return null;
        }
        double lon = point.optDouble(0, Double.NaN);
        double lat = point.optDouble(1, Double.NaN);
        if (!LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        return new LatLon(lat, lon);
    }

    private static double parseRouteMetricDouble(@NonNull String s) {
        try {
            double parsed = Double.parseDouble(s);
            return Double.isFinite(parsed) && parsed >= 0.0 ? parsed : 0.0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
