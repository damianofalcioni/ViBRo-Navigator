package vibro.navigator.poi.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import vibro.navigator.geo.LatLon;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiAddressLabel;
import vibro.navigator.poi.PoiDetails;

final class GoogleGeocodeResponseParser {
    private static final String KEY_ADDRESS_COMPONENTS = "address_components";
    private static final String KEY_FORMATTED_ADDRESS = "formatted_address";
    private static final String KEY_GEOMETRY = "geometry";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_RESULTS = "results";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_REQUEST_DENIED = "REQUEST_DENIED";

    private GoogleGeocodeResponseParser() {
    }

    @NonNull
    static List<Poi> parseResults(@NonNull String body, int limit) throws JSONException {
        JSONObject root = new JSONObject(body);
        JSONArray results = root.optJSONArray(KEY_RESULTS);
        List<Poi> out = new ArrayList<>();
        if (results == null) {
            return out;
        }
        for (int i = 0; i < results.length() && out.size() < limit; i++) {
            Poi poi = parseResult(results.optJSONObject(i));
            if (poi != null) {
                out.add(poi);
            }
        }
        return out;
    }

    @NonNull
    static String parseStatus(@NonNull String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        return root.optString("status", "");
    }

    static boolean isOkStatus(@NonNull String body) throws JSONException {
        return STATUS_OK.equals(parseStatus(body));
    }

    static boolean isRequestDeniedStatus(@NonNull String body) throws JSONException {
        return STATUS_REQUEST_DENIED.equals(parseStatus(body));
    }

    @Nullable
    static String parseFirstFormattedAddress(@NonNull String body) throws JSONException {
        JSONObject root = new JSONObject(body);
        JSONArray results = root.optJSONArray(KEY_RESULTS);
        if (results == null || results.length() == 0) {
            return null;
        }
        JSONObject firstResult = results.optJSONObject(0);
        if (firstResult == null) {
            return null;
        }
        String address = parseResultLabel(firstResult);
        return address.isEmpty() ? null : address;
    }

    private static Poi parseResult(JSONObject result) {
        if (result == null) {
            return null;
        }
        Map<String, String> addressDetails = GoogleGeocodeAddressComponents.parse(
                result.optJSONArray(KEY_ADDRESS_COMPONENTS)
        );
        String name = parseResultLabel(result, addressDetails);
        JSONObject geometry = result.optJSONObject(KEY_GEOMETRY);
        if (geometry == null) {
            return null;
        }
        JSONObject location = geometry.optJSONObject(KEY_LOCATION);
        if (location == null) {
            return null;
        }
        double lat = location.optDouble("lat", Double.NaN);
        double lon = location.optDouble("lng", Double.NaN);
        if (name.isEmpty() || !LatLon.isValidCoordinate(lat, lon)) {
            return null;
        }
        return new Poi(name, lat, lon, poiDetails(addressDetails));
    }

    @NonNull
    private static String parseResultLabel(@NonNull JSONObject result) {
        return parseResultLabel(
                result,
                GoogleGeocodeAddressComponents.parse(result.optJSONArray(KEY_ADDRESS_COMPONENTS))
        );
    }

    @NonNull
    private static String parseResultLabel(
            @NonNull JSONObject result,
            @NonNull Map<String, String> addressDetails
    ) {
        return PoiAddressLabel.conciseLabel(
                result.optString(KEY_FORMATTED_ADDRESS, ""),
                addressDetails
        );
    }

    @Nullable
    private static PoiDetails poiDetails(@NonNull Map<String, String> addressDetails) {
        if (addressDetails.isEmpty()) {
            return null;
        }
        return new PoiDetails(addressDetails, Collections.emptyMap());
    }
}
