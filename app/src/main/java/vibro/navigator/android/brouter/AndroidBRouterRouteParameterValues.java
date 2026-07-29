package vibro.navigator.android.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

final class AndroidBRouterRouteParameterValues {
    @NonNull
    private final Map<String, Object> values;

    private AndroidBRouterRouteParameterValues(@NonNull Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    @NonNull
    static AndroidBRouterRouteParameterValues build(@NonNull BRouterRouteRequest request) {
        return build(request, null);
    }

    @NonNull
    static AndroidBRouterRouteParameterValues build(
            @NonNull BRouterRouteRequest request,
            @Nullable String remoteProfile
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        List<LatLon> points = routePoints(request);

        putRoutePoints(values, request, points);
        putNogos(values, request.blockedWaypoints);
        putProfile(values, request, remoteProfile);
        if (request.profileParameters != null) {
            values.put("extraParams", request.profileParameters);
        }
        putRoundTripParams(values, request);

        // GeoJSON output from BRouter is called "json" and follows GeoJSON FeatureCollection.
        values.put("format", "json");
        values.put("trackFormat", "json");
        // Use BRouter-native turn hints so exits and beeline hints remain distinct.
        values.put("timode", "9");
        values.put("acceptCompressedResult", "true");
        return new AndroidBRouterRouteParameterValues(values);
    }

    @NonNull
    Set<Map.Entry<String, Object>> entries() {
        return values.entrySet();
    }

    boolean containsKey(@NonNull String key) {
        return values.containsKey(key);
    }

    @Nullable
    double[] getDoubleArray(@NonNull String key) {
        Object value = values.get(key);
        return value instanceof double[] ? (double[]) value : null;
    }

    @Nullable
    String getString(@NonNull String key) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : null;
    }

    int getInt(@NonNull String key) {
        Object value = values.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalArgumentException("BRouter route parameter is not an integer: " + key);
    }

    private static void putProfile(
            @NonNull Map<String, Object> values,
            @NonNull BRouterRouteRequest request,
            @Nullable String remoteProfile
    ) {
        if (!request.customProfile) {
            values.put("profile", request.profile);
            return;
        }
        if (remoteProfile == null || remoteProfile.trim().isEmpty()) {
            throw new IllegalStateException("Custom BRouter profile is unavailable");
        }
        values.put("remoteProfile", remoteProfile);
    }

    private static void putRoutePoints(
            @NonNull Map<String, Object> values,
            @NonNull BRouterRouteRequest request,
            @NonNull List<LatLon> points
    ) {
        if (request.isRoundTrip()) {
            values.put("lonlats", lonLat(points.get(0)));
            return;
        }
        values.put("lats", latitudes(points));
        values.put("lons", longitudes(points));
    }

    private static void putRoundTripParams(
            @NonNull Map<String, Object> values,
            @NonNull BRouterRouteRequest request
    ) {
        if (!request.isRoundTrip()) {
            return;
        }
        values.put("engineMode", 4);
        values.put("direction", request.roundTripDirectionDegrees);
        values.put("roundTripDistance", request.roundTripDistanceMeters);
    }

    @NonNull
    private static List<LatLon> routePoints(@NonNull BRouterRouteRequest request) {
        List<LatLon> points = new ArrayList<>();
        points.add(request.start);
        if (request.isRoundTrip()) {
            return points;
        }
        points.addAll(request.intermediates);
        points.add(requireDestination(request));
        return points;
    }

    @NonNull
    private static LatLon requireDestination(@NonNull BRouterRouteRequest request) {
        if (request.destination == null) {
            throw new IllegalStateException("BRouter route request is missing a destination");
        }
        return request.destination;
    }

    @NonNull
    private static String lonLat(@NonNull LatLon point) {
        return Double.toString(point.lon) + "," + Double.toString(point.lat);
    }

    @NonNull
    private static double[] latitudes(@NonNull List<LatLon> points) {
        double[] lats = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            lats[i] = points.get(i).lat;
        }
        return lats;
    }

    @NonNull
    private static double[] longitudes(@NonNull List<LatLon> points) {
        double[] lons = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            lons[i] = points.get(i).lon;
        }
        return lons;
    }

    private static void putNogos(@NonNull Map<String, Object> values, @NonNull List<NogoPoint> nogos) {
        double[] nogoLats = new double[nogos.size()];
        double[] nogoLons = new double[nogos.size()];
        double[] nogoRadi = new double[nogos.size()];
        for (int i = 0; i < nogos.size(); i++) {
            NogoPoint nogo = nogos.get(i);
            nogoLats[i] = nogo.lat;
            nogoLons[i] = nogo.lon;
            nogoRadi[i] = nogo.radiusMeters;
        }
        values.put("nogoLats", nogoLats);
        values.put("nogoLons", nogoLons);
        values.put("nogoRadi", nogoRadi);
    }
}
