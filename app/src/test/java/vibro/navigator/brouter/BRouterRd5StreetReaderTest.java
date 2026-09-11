package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;

public class BRouterRd5StreetReaderTest {
    private static final double LATITUDE = 48.18773346166013d;
    private static final double LONGITUDE = 16.38147556524725d;

    @Test
    public void read_atCellBoundaryKeepsStreetsOnEverySideWhenFirstCellExceedsBudget() throws IOException {
        List<CompassStreetSegment> segments = load(8);

        assertEquals(8, segments.size());
        int[] quadrants = new int[4];
        for (CompassStreetSegment segment : segments) {
            LatLon point = segment.points.get(0);
            quadrants[(point.lat > LATITUDE ? 2 : 0) + (point.lon > LONGITUDE ? 1 : 0)]++;
        }
        for (int count : quadrants) {
            assertTrue("Streets should survive in every direction", count > 0);
        }
    }

    @Test
    public void read_belowBudgetPreservesDecodedCoordinatesAndStreetTypes() throws IOException {
        List<CompassStreetSegment> segments = load(100);

        assertEquals(32, segments.size());
        for (CompassStreetSegment segment : segments) {
            assertEquals(CompassStreetType.RESIDENTIAL, segment.type);
            assertEquals(2, segment.points.size());
            LatLon start = segment.points.get(0);
            LatLon end = segment.points.get(1);
            assertEquals(start.lat, end.lat, 0.000000001d);
            assertEquals(start.lon + 0.000005d, end.lon, 0.000000001d);
        }
    }

    private static List<CompassStreetSegment> load(int limit) throws IOException {
        List<CompassStreetSegment> segments = new ArrayList<>();
        try (BRouterStreetTestFile file = new BRouterStreetTestFile()) {
            new BRouterRd5StreetReader(file, "E15_N45.rd5").read(
                    BRouterSegmentBounds.around(LATITUDE, LONGITUDE, 360d), limit, segments
            );
        }
        return segments;
    }
}
