package com.vibenavigator.nav.route;

import androidx.annotation.NonNull;

import com.vibenavigator.geo.LatLon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GeoJsonRouteParser {
    private GeoJsonRouteParser() {
    }

    @NonNull
    public static GeoJsonRoute parse(@NonNull String geoJson) {
        try {
            JSONObject root = new JSONObject(geoJson);
            JSONArray features = root.optJSONArray("features");
            if (features == null || features.length() == 0) {
                return new GeoJsonRoute(new ArrayList<>(), new ArrayList<>(), 0, 0);
            }

            JSONObject trackFeature = features.optJSONObject(0);
            if (trackFeature == null) {
                return new GeoJsonRoute(new ArrayList<>(), new ArrayList<>(), 0, 0);
            }

            JSONObject props = trackFeature.optJSONObject("properties");
            double totalTime = parseDouble(props != null ? props.optString("total-time", "0") : "0");
            double trackLen = parseDouble(props != null ? props.optString("track-length", "0") : "0");

            List<VoiceHint> voiceHints = new ArrayList<>();
            if (props != null) {
                JSONArray vh = props.optJSONArray("voicehints");
                if (vh != null) {
                    for (int i = 0; i < vh.length(); i++) {
                        JSONArray hint = vh.optJSONArray(i);
                        if (hint == null || hint.length() < 5) {
                            continue;
                        }
                        int indexInTrack = hint.optInt(0, -1);
                        int cmd = hint.optInt(1, 0);
                        int exit = hint.optInt(2, 0);
                        double distNext = hint.optDouble(3, 0);
                        int angle = hint.optInt(4, 0);
                        if (indexInTrack < 0) {
                            continue;
                        }
                        voiceHints.add(new VoiceHint(indexInTrack, cmd, exit, distNext, angle));
                    }
                }
            }

            JSONObject geom = trackFeature.optJSONObject("geometry");
            List<LatLon> track = new ArrayList<>();
            if (geom != null) {
                JSONArray coords = geom.optJSONArray("coordinates");
                if (coords != null) {
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray p = coords.optJSONArray(i);
                        if (p == null || p.length() < 2) {
                            continue;
                        }
                        double lon = p.optDouble(0, Double.NaN);
                        double lat = p.optDouble(1, Double.NaN);
                        if (Double.isNaN(lat) || Double.isNaN(lon)) {
                            continue;
                        }
                        track.add(new LatLon(lat, lon));
                    }
                }
            }

            return new GeoJsonRoute(track, voiceHints, totalTime, trackLen);
        } catch (JSONException ignored) {
            return new GeoJsonRoute(new ArrayList<>(), new ArrayList<>(), 0, 0);
        }
    }

    private static double parseDouble(@NonNull String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
