package vibro.navigator.android.brouter;

import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

final class AndroidBRouterParams {
    private AndroidBRouterParams() {
    }

    @NonNull
    static Bundle buildRouteParams(@NonNull BRouterRouteRequest request) {
        List<LatLon> pts = new ArrayList<>();
        pts.add(request.start);
        pts.addAll(request.intermediates);
        pts.add(request.destination);

        Bundle b = new Bundle();
        b.putDoubleArray("lats", latitudes(pts));
        b.putDoubleArray("lons", longitudes(pts));
        putNogos(b, request.blockedWaypoints);
        b.putString("profile", request.profile);

        // GeoJSON output from BRouter is called "json" and follows GeoJSON FeatureCollection.
        b.putString("format", "json");
        b.putString("trackFormat", "json");

        // Use BRouter-native turn hints so exits and beeline hints remain distinct.
        b.putString("timode", "9");
        b.putString("acceptCompressedResult", "true");
        return b;
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

    private static void putNogos(@NonNull Bundle b, @NonNull List<NogoPoint> nogos) {
        double[] nogoLats = new double[nogos.size()];
        double[] nogoLons = new double[nogos.size()];
        double[] nogoRadi = new double[nogos.size()];
        for (int i = 0; i < nogos.size(); i++) {
            NogoPoint nogo = nogos.get(i);
            nogoLats[i] = nogo.lat;
            nogoLons[i] = nogo.lon;
            nogoRadi[i] = nogo.radiusMeters;
        }
        b.putDoubleArray("nogoLats", nogoLats);
        b.putDoubleArray("nogoLons", nogoLons);
        b.putDoubleArray("nogoRadi", nogoRadi);
    }
}
