package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import vibro.navigator.nav.compass.CompassStreetSegment;

public class BRouterDecodedStreetReaderTest {
    private static final String SOURCE = "source";
    private final BRouterDecodedStreetCache cache = new BRouterDecodedStreetCache();
    private final BRouterStreetTestFile file = new BRouterStreetTestFile();

    @Test
    public void neighboringQueryReusesWholeCellsRatherThanThePreviousSample() throws IOException {
        assertEquals(1, read(cache, SOURCE, 360d, 1).size());
        assertEquals(2, file.cellReads);

        List<CompassStreetSegment> warm = read(cache, SOURCE, 300d, 100);

        assertEquals(2, file.cellReads);
        assertEquals(32, warm.size());
        List<CompassStreetSegment> cold = read(new BRouterDecodedStreetCache(), SOURCE, 300d, 100);
        assertEquals(geometry(cold), geometry(warm));
    }

    @Test
    public void neighboringQueryReusesMicrocacheIndexPositionsForSameRevision() throws IOException {
        BRouterRd5MetadataCache metadata = new BRouterRd5MetadataCache();
        read(cache, metadata, SOURCE, 360d, 1);
        int indexReads = file.indexReads;

        read(cache, metadata, SOURCE, 300d, 100);

        assertEquals(indexReads, file.indexReads);
    }

    @Test
    public void replacementMapRevisionAndStorageSourceDoNotReuseOldCells() throws IOException {
        read(cache, SOURCE, 360d, 100);
        file.changeRevision(1L);
        read(cache, SOURCE, 360d, 100);
        assertEquals(4, file.cellReads);

        read(cache, "other source", 360d, 100);
        assertEquals(6, file.cellReads);
    }

    @Test
    public void oversizedCellsStillReturnCompleteCoverageWithoutBeingCached() throws IOException {
        BRouterDecodedStreetCache tiny = new BRouterDecodedStreetCache(16);
        assertEquals(32, read(tiny, SOURCE, 360d, 100).size());
        assertEquals(32, read(tiny, SOURCE, 360d, 100).size());
        assertEquals(4, file.cellReads);
    }

    @Test
    public void invalidChecksumNeverPopulatesTheCache() {
        file.corruptFirstCell();
        assertThrows(IOException.class, () -> read(cache, SOURCE, 360d, 100));
        assertThrows(IOException.class, () -> read(cache, SOURCE, 360d, 100));
        assertEquals(2, file.cellReads);
    }

    @Test
    public void interruptedLoadDoesNotReadData() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> read(cache, SOURCE, 360d, 100));
        } finally {
            Thread.interrupted();
        }
        assertEquals(0, file.cellReads);
    }

    private List<CompassStreetSegment> read(BRouterDecodedStreetCache cells, String source, double radius, int limit)
            throws IOException {
        return read(cells, new BRouterRd5MetadataCache(), source, radius, limit);
    }

    private List<CompassStreetSegment> read(
            BRouterDecodedStreetCache cells,
            BRouterRd5MetadataCache metadata,
            String source,
            double radius,
            int limit
    ) throws IOException {
        List<CompassStreetSegment> out = new ArrayList<>();
        new BRouterRd5StreetReader(file, "E15_N45.rd5", source, cells, metadata).read(
                BRouterSegmentBounds.around(48.18773346166013d, 16.38147556524725d, radius), limit, out
        );
        return out;
    }

    private static List<String> geometry(List<CompassStreetSegment> segments) {
        List<String> geometry = new ArrayList<>();
        for (CompassStreetSegment segment : segments) {
            geometry.add(segment.type + ":" + segment.points);
        }
        return geometry;
    }
}
