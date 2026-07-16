package vibro.navigator.android.brouter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

final class AndroidBRouterRouteParams {
    private AndroidBRouterRouteParams() {
    }

    @NonNull
    static Bundle build(@NonNull BRouterRouteRequest request) {
        return build(request, null);
    }

    @NonNull
    static Bundle build(@NonNull BRouterRouteRequest request, @Nullable String remoteProfile) {
        List<LatLon> points = routePoints(request);

        Bundle bundle = new Bundle();
        putRoutePoints(bundle, request, points);
        putNogos(bundle, request.blockedWaypoints);
        putProfile(bundle, request, remoteProfile);
        if (request.profileParameters != null) {
            bundle.putString("extraParams", request.profileParameters);
        }
        putRoundTripParams(bundle, request);

        // GeoJSON output from BRouter is called "json" and follows GeoJSON FeatureCollection.
        bundle.putString("format", "json");
        bundle.putString("trackFormat", "json");

        // Use BRouter-native turn hints so exits and beeline hints remain distinct.
        bundle.putString("timode", "9");
        bundle.putString("acceptCompressedResult", "true");
        return bundle;
    }

    private static void putProfile(
            @NonNull Bundle bundle,
            @NonNull BRouterRouteRequest request,
            @Nullable String remoteProfile
    ) {
        if (!request.customProfile) {
            bundle.putString("profile", request.profile);
            return;
        }
        if (remoteProfile == null || remoteProfile.trim().isEmpty()) {
            throw new IllegalStateException("Custom BRouter profile is unavailable");
        }
        bundle.putString("remoteProfile", remoteProfile);
    }

    private static void putRoutePoints(
            @NonNull Bundle bundle,
            @NonNull BRouterRouteRequest request,
            @NonNull List<LatLon> points
    ) {
        if (request.isRoundTrip()) {
            bundle.putString("lonlats", lonLat(points.get(0)));
            return;
        }
        bundle.putDoubleArray("lats", latitudes(points));
        bundle.putDoubleArray("lons", longitudes(points));
    }

    private static void putRoundTripParams(
            @NonNull Bundle bundle,
            @NonNull BRouterRouteRequest request
    ) {
        if (!request.isRoundTrip()) {
            return;
        }
        bundle.putInt("engineMode", 4);
        bundle.putInt("direction", request.roundTripDirectionDegrees);
        bundle.putInt("roundTripDistance", request.roundTripDistanceMeters);
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

    private static void putNogos(@NonNull Bundle bundle, @NonNull List<NogoPoint> nogos) {
        double[] nogoLats = new double[nogos.size()];
        double[] nogoLons = new double[nogos.size()];
        double[] nogoRadi = new double[nogos.size()];
        for (int i = 0; i < nogos.size(); i++) {
            NogoPoint nogo = nogos.get(i);
            nogoLats[i] = nogo.lat;
            nogoLons[i] = nogo.lon;
            nogoRadi[i] = nogo.radiusMeters;
        }
        bundle.putDoubleArray("nogoLats", nogoLats);
        bundle.putDoubleArray("nogoLons", nogoLons);
        bundle.putDoubleArray("nogoRadi", nogoRadi);
    }
}
