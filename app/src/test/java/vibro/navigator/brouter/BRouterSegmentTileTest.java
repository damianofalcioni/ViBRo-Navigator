package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

public class BRouterSegmentTileTest {
    @Test
    public void fileNameFor_matchesBRouterSegments4NamingExamples() {
        assertEquals("W50_N35.rd5", BRouterSegmentTile.fileNameFor(37.0d, -48.0d));
        assertEquals("E5_N45.rd5", BRouterSegmentTile.fileNameFor(47.0d, 7.0d));
    }

    @Test
    public void fileNamesForBounds_includesNeighborTileWhenRadiusCrossesBoundary() {
        BRouterSegmentBounds bounds = BRouterSegmentBounds.around(47.0d, 4.999d, 300.0d);

        Set<String> names = BRouterSegmentTile.fileNamesForBounds(bounds);

        assertTrue(names.contains("E0_N45.rd5"));
        assertTrue(names.contains("E5_N45.rd5"));
    }
}
