package vibro.navigator.nav.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small relative-coordinate format shared by motion/filter and acquisition regressions. */
public final class LocationLogReplay {
    private LocationLogReplay() { }

    public static List<Sample> load(String name) throws IOException {
        InputStream input = LocationLogReplay.class.getResourceAsStream("/location-replay/" + name + ".trace");
        if (input == null) {
            throw new IOException("Missing replay " + name);
        }
        List<Sample> samples = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    samples.add(parse(line.split("\\s+")));
                }
            }
        }
        return samples;
    }

    private static Sample parse(String[] fields) {
        NavigationLocation location = new NavigationLocation(fields[2]);
        long time = Long.parseLong(fields[0]);
        location.setTime(time, time);
        location.setLatitude(Double.parseDouble(fields[3]) / 111_320);
        location.setLongitude(Double.parseDouble(fields[4]) / 111_320);
        location.setAccuracy(Float.parseFloat(fields[5]));
        if (Float.isFinite(Float.parseFloat(fields[6]))) {
            location.setSpeed(Float.parseFloat(fields[6]));
        }
        if (Float.isFinite(Float.parseFloat(fields[7]))) {
            location.setSpeedAccuracyMetersPerSecond(Float.parseFloat(fields[7]));
        }
        return new Sample(location, Long.parseLong(fields[1]), Long.parseLong(fields[8]));
    }

    public static final class Sample {
        public final NavigationLocation location;
        public final long receivedAtMs;
        public final long requestedIntervalMs;

        private Sample(NavigationLocation location, long receivedAtMs, long requestedIntervalMs) {
            this.location = location;
            this.receivedAtMs = receivedAtMs;
            this.requestedIntervalMs = requestedIntervalMs;
        }
    }
}
