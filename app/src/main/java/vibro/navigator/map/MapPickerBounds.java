package vibro.navigator.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

final class MapPickerBounds {
    final double south;
    final double west;
    final double north;
    final double east;
    final int zoom;

    private MapPickerBounds(double south, double west, double north, double east, int zoom) {
        this.south = south;
        this.west = west;
        this.north = north;
        this.east = east;
        this.zoom = zoom;
    }

    @NonNull
    static MapPickerBounds of(double south, double west, double north, double east, int zoom) {
        return new MapPickerBounds(south, west, north, east, zoom);
    }

    @Nullable
    static MapPickerBounds parseJavascriptResult(@Nullable String value) throws JSONException {
        if (value == null || "null".equals(value)) {
            return null;
        }
        String json = new JSONArray("[" + value + "]").optString(0, "");
        if (json.isEmpty()) {
            return null;
        }
        JSONObject object = new JSONObject(json);
        return new MapPickerBounds(
                object.getDouble("south"),
                object.getDouble("west"),
                object.getDouble("north"),
                object.getDouble("east"),
                object.getInt("zoom")
        );
    }

    boolean isReadyForPoiFetch() {
        return north > south
                && east > west
                && north <= 85.05112878d
                && south >= -85.05112878d;
    }

    boolean intersects(@NonNull MapPickerBounds other) {
        return south < other.north
                && north > other.south
                && west < other.east
                && east > other.west;
    }

    boolean contains(double lat, double lon) {
        return lat >= south && lat <= north && lon >= west && lon <= east;
    }

    @NonNull
    String overpassBbox() {
        return String.format(Locale.US, "%.7f,%.7f,%.7f,%.7f", south, west, north, east);
    }
}
