package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

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

            JSONObject trackFeature = features.optJSONObject(0);
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
        return new GeoJsonRoute(
                parseTrack(trackFeature.optJSONObject("geometry")),
                parseVoiceHints(props),
                parseTimes(props != null ? props.optJSONArray("times") : null),
                GeoJsonRouteSpeedLimitParser.parse(props != null ? props.optJSONArray("messages") : null),
                parseDouble(routeMetric(props, "total-time")),
                parseDouble(routeMetric(props, "track-length"))
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
    private static List<VoiceHint> parseVoiceHints(JSONObject props) {
        List<VoiceHint> voiceHints = new ArrayList<>();
        JSONArray rawHints = props != null ? props.optJSONArray("voicehints") : null;
        if (rawHints == null) {
            return voiceHints;
        }
        for (int i = 0; i < rawHints.length(); i++) {
            VoiceHint voiceHint = parseVoiceHint(rawHints.optJSONArray(i));
            if (voiceHint != null) {
                voiceHints.add(voiceHint);
            }
        }
        return voiceHints;
    }

    private static VoiceHint parseVoiceHint(JSONArray hint) {
        if (hint == null || hint.length() < 5) {
            return null;
        }
        int indexInTrack = hint.optInt(0, -1);
        if (indexInTrack < 0) {
            return null;
        }
        return new VoiceHint(
                indexInTrack,
                hint.optInt(1, 0),
                hint.optInt(2, 0),
                hint.optDouble(3, 0),
                hint.optInt(4, 0)
        );
    }

    @NonNull
    private static List<LatLon> parseTrack(JSONObject geom) {
        List<LatLon> track = new ArrayList<>();
        JSONArray coords = geom != null ? geom.optJSONArray("coordinates") : null;
        if (coords == null) {
            return track;
        }
        for (int i = 0; i < coords.length(); i++) {
            LatLon point = parseTrackPoint(coords.optJSONArray(i));
            if (point != null) {
                track.add(point);
            }
        }
        return track;
    }

    private static LatLon parseTrackPoint(JSONArray point) {
        if (point == null || point.length() < 2) {
            return null;
        }
        double lon = point.optDouble(0, Double.NaN);
        double lat = point.optDouble(1, Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }
        return new LatLon(lat, lon);
    }

    @NonNull
    private static List<Double> parseTimes(JSONArray timesArray) {
        List<Double> out = new ArrayList<>();
        if (timesArray == null) {
            return out;
        }
        for (int i = 0; i < timesArray.length(); i++) {
            Object raw = timesArray.opt(i);
            double parsed = parseDouble(String.valueOf(raw));
            if (!Double.isFinite(parsed)) {
                continue;
            }
            out.add(parsed);
        }
        return out;
    }

    private static double parseDouble(@NonNull String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
