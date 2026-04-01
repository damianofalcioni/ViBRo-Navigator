package com.vibenavigator.brouter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.geo.LatLon;

import java.util.ArrayList;
import java.util.List;

public final class BRouterParams {

    public enum VehicleMode {
        MOTORCAR("motorcar"),
        BICYCLE("bicycle"),
        FOOT("foot");

        public final String brouterValue;

        VehicleMode(@NonNull String v) {
            brouterValue = v;
        }
    }

    private BRouterParams() {
    }

    @NonNull
    public static Bundle buildRouteParams(
            @NonNull LatLon start,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon end,
            @NonNull String profile,
            @NonNull VehicleMode mode,
            boolean fast,
            @Nullable List<LatLon> nogos
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
                nogoLats[i] = nogos.get(i).lat;
                nogoLons[i] = nogos.get(i).lon;
                nogoRadi[i] = 10.0;
            }
            b.putDoubleArray("nogoLats", nogoLats);
            b.putDoubleArray("nogoLons", nogoLons);
            b.putDoubleArray("nogoRadi", nogoRadi);
        } else {
            b.putDoubleArray("nogoLats", new double[0]);
            b.putDoubleArray("nogoLons", new double[0]);
            b.putDoubleArray("nogoRadi", new double[0]);
        }

        b.putString("fast", fast ? "1" : "0");
        b.putString("v", mode.brouterValue);
        b.putString("profile", profile);

        // GeoJSON output from BRouter is called "json" and follows GeoJSON FeatureCollection.
        b.putString("format", "json");
        b.putString("trackFormat", "json");

        // Ensure voice hints are included (osmand style -> timode 3).
        b.putString("turnInstructionFormat", "osmand");
        b.putString("acceptCompressedResult", "true");

        return b;
    }
}
