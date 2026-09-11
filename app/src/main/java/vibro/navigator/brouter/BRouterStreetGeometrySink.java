package vibro.navigator.brouter;

import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassStreetType;

interface BRouterStreetGeometrySink {
    void offer(List<LatLon> points, CompassStreetType type);
}
