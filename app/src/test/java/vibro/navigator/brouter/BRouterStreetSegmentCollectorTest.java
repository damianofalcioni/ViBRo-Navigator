package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.EnumSet;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetVisibility;

public class BRouterStreetSegmentCollectorTest {
    @Test
    public void offer_denseFirstAreaDoesNotCrowdOutLaterDirections() {
        BRouterStreetSegmentCollector collector = collector(16);
        addStreets(collector, -0.002d, -0.002d, 100);
        addStreets(collector, -0.002d, 0.002d, 100);
        addStreets(collector, 0.002d, -0.002d, 100);
        addStreets(collector, 0.002d, 0.002d, 100);

        List<CompassStreetSegment> selected = selected(collector);

        assertEquals(16, selected.size());
        int[] quadrants = new int[4];
        for (CompassStreetSegment segment : selected) {
            LatLon point = segment.points.get(0);
            quadrants[(point.lat > 0 ? 2 : 0) + (point.lon > 0 ? 1 : 0)]++;
        }
        for (int count : quadrants) {
            assertEquals(4, count);
        }
    }

    @Test
    public void offer_sparseAreaKeepsEveryStreetUpToBudget() {
        BRouterStreetSegmentCollector collector = collector(16);
        addStreets(collector, 0.002d, 0.002d, 12);

        assertEquals(12, selected(collector).size());
    }

    @Test
    public void offer_fullBudgetStillAcceptsCloserStreetsInSameArea() {
        BRouterStreetSegmentCollector collector = collector(1);
        addStreets(collector, 0.003d, 0.003d, 1);
        addStreets(collector, 0.002d, 0.002d, 1);

        assertEquals(0.002d, selected(collector).get(0).points.get(0).lat, 0d);
    }

    @Test
    public void offer_ignoresOutsideAreaAndEmptyBudgets() {
        BRouterStreetSegmentCollector collector = collector(16);
        addStreets(collector, 1d, 1d, 10);
        assertTrue(selected(collector).isEmpty());

        BRouterStreetSegmentCollector empty = collector(0);
        addStreets(empty, 0d, 0d, 10);
        assertTrue(selected(empty).isEmpty());
    }

    @Test
    public void offer_filtersTypesAndCategoriesBeforeSpatialSamplingBudget() {
        CompassStreetVisibility selection = new CompassStreetVisibility(
                EnumSet.of(CompassStreetCategory.NORMAL),
                EnumSet.of(CompassStreetType.RAILWAY)
        );
        BRouterStreetSegmentCollector collector = new BRouterStreetSegmentCollector(
                BRouterSegmentBounds.around(0d, 0d, 360d), 2, selection
        );
        List<LatLon> points = Arrays.asList(new LatLon(0d, 0d), new LatLon(0.0001d, 0d));
        for (int i = 0; i < 100; i++) {
            collector.offer(points, CompassStreetType.RESIDENTIAL);
            collector.offer(points, CompassStreetType.RAILWAY);
        }
        collector.offer(points, CompassStreetType.WATERWAY);
        collector.offer(points, CompassStreetType.ROUTE_FERRY);

        List<CompassStreetSegment> selected = selected(collector);
        assertEquals(2, selected.size());
        assertTrue(selected.stream().anyMatch(segment -> segment.type == CompassStreetType.WATERWAY));
        assertTrue(selected.stream().anyMatch(segment -> segment.type == CompassStreetType.ROUTE_FERRY));
    }

    @Test
    public void offer_buswayFollowsSpecialCategoryAtExtraction() {
        List<LatLon> points = Arrays.asList(new LatLon(0d, 0d), new LatLon(0.0001d, 0d));
        BRouterStreetSegmentCollector normalOff = new BRouterStreetSegmentCollector(
                BRouterSegmentBounds.around(0d, 0d, 360d), 2,
                new CompassStreetVisibility(
                        EnumSet.of(CompassStreetCategory.NORMAL),
                        EnumSet.noneOf(CompassStreetType.class)
                )
        );
        normalOff.offer(points, CompassStreetType.BUSWAY);
        assertEquals(1, selected(normalOff).size());

        BRouterStreetSegmentCollector specialOff = new BRouterStreetSegmentCollector(
                BRouterSegmentBounds.around(0d, 0d, 360d), 2,
                new CompassStreetVisibility(
                        EnumSet.of(CompassStreetCategory.SPECIAL_ROUTING),
                        EnumSet.noneOf(CompassStreetType.class)
                )
        );
        specialOff.offer(points, CompassStreetType.BUSWAY);
        assertTrue(selected(specialOff).isEmpty());
    }

    private static BRouterStreetSegmentCollector collector(int limit) {
        return new BRouterStreetSegmentCollector(BRouterSegmentBounds.around(0d, 0d, 360d), limit);
    }

    private static void addStreets(BRouterStreetSegmentCollector collector, double lat, double lon, int count) {
        for (int i = 0; i < count; i++) {
            collector.offer(Arrays.asList(
                    new LatLon(lat + i * 0.000001d, lon),
                    new LatLon(lat + i * 0.000001d, lon + 0.00001d)
            ), CompassStreetType.RESIDENTIAL);
        }
    }

    private static List<CompassStreetSegment> selected(BRouterStreetSegmentCollector collector) {
        List<CompassStreetSegment> out = new ArrayList<>();
        collector.appendTo(out);
        return out;
    }
}
