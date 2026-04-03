package com.vibenavigator.brouter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.LatLon;

import java.util.ArrayList;
import java.util.List;

public final class BRouterParams {

    private BRouterParams() {
    }

    @NonNull
    public static Bundle buildRouteParams(
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @Nullable List<NogoPoint> nogos
    ) {
        List<LatLon> pts = new ArrayList<>();
        pts.add(start);
        pts.addAll(intermediates);
        pts.add(end);

        double[] lats = new double[pts.size()];
        double[] lons = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            lats[i] = pts.get(i).lat;
            lons[i] = pts.get(i).lon;
        }

        Bundle b = new Bundle();
        b.putDoubleArray("lats", lats);
        b.putDoubleArray("lons", lons);

        if (nogos != null && !nogos.isEmpty()) {
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
        } else {
            b.putDoubleArray("nogoLats", new double[0]);
            b.putDoubleArray("nogoLons", new double[0]);
            b.putDoubleArray("nogoRadi", new double[0]);
        }

        b.putString("profile", profile);

        // GeoJSON output from BRouter is called "json" and follows GeoJSON FeatureCollection.
        b.putString("format", "json");
        b.putString("trackFormat", "json");

        // Use BRouter-native turn hints so exits and beeline hints remain distinct.
        b.putString("timode", "9");
        b.putString("acceptCompressedResult", "true");

        return b;
    }
}
