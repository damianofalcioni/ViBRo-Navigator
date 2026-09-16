package vibro.navigator.nav.compass;

public enum CompassStreetType {
    MOTORWAY(CompassStreetCategory.HIGHWAY),
    MOTORWAY_LINK(CompassStreetCategory.HIGHWAY),
    TRUNK(CompassStreetCategory.HIGHWAY),
    TRUNK_LINK(CompassStreetCategory.HIGHWAY),
    PRIMARY(CompassStreetCategory.HIGHWAY),
    PRIMARY_LINK(CompassStreetCategory.HIGHWAY),
    SECONDARY(CompassStreetCategory.HIGHWAY),
    SECONDARY_LINK(CompassStreetCategory.HIGHWAY),
    TERTIARY(CompassStreetCategory.NORMAL),
    TERTIARY_LINK(CompassStreetCategory.NORMAL),
    UNCLASSIFIED(CompassStreetCategory.NORMAL),
    RESIDENTIAL(CompassStreetCategory.NORMAL),
    LIVING_STREET(CompassStreetCategory.WALKING_CYCLING),
    SERVICE(CompassStreetCategory.NORMAL),
    TRACK(CompassStreetCategory.WALKING_CYCLING),
    ROAD(CompassStreetCategory.NORMAL),
    BUSWAY(CompassStreetCategory.SPECIAL_ROUTING),
    PEDESTRIAN(CompassStreetCategory.WALKING_CYCLING),
    FOOTWAY(CompassStreetCategory.WALKING_CYCLING),
    PATH(CompassStreetCategory.WALKING_CYCLING),
    CYCLEWAY(CompassStreetCategory.WALKING_CYCLING),
    BRIDLEWAY(CompassStreetCategory.SPECIAL_ROUTING),
    STEPS(CompassStreetCategory.WALKING_CYCLING),
    PLATFORM(CompassStreetCategory.WALKING_CYCLING),
    CORRIDOR(CompassStreetCategory.WALKING_CYCLING),
    REST_AREA(CompassStreetCategory.HIGHWAY),
    SERVICES(CompassStreetCategory.HIGHWAY),
    ELEVATOR(CompassStreetCategory.WALKING_CYCLING),
    VIA_FERRATA(CompassStreetCategory.SPECIAL_ROUTING),
    RACEWAY(CompassStreetCategory.SPECIAL_ROUTING),
    OTHER(CompassStreetCategory.SPECIAL_ROUTING),
    ROUTE_WALKING_CYCLING(CompassStreetCategory.WALKING_CYCLING),
    RAILWAY(CompassStreetCategory.SPECIAL_ROUTING),
    WATERWAY(CompassStreetCategory.SPECIAL_ROUTING),
    SPECIAL_ROUTE(CompassStreetCategory.SPECIAL_ROUTING),
    ROUTE_FERRY(CompassStreetCategory.SPECIAL_ROUTING),
    ROUTE_HIKING_FOOT(CompassStreetCategory.WALKING_CYCLING),
    ROUTE_BICYCLE(CompassStreetCategory.WALKING_CYCLING),
    ROUTE_SKI_PISTE(CompassStreetCategory.SPECIAL_ROUTING),
    ROUTE_MTB(CompassStreetCategory.WALKING_CYCLING),
    ROUTE_CANOE(CompassStreetCategory.SPECIAL_ROUTING),
    ROUTE_BUS(CompassStreetCategory.SPECIAL_ROUTING);

    private final CompassStreetCategory category;

    CompassStreetType(CompassStreetCategory category) {
        this.category = category;
    }

    public CompassStreetCategory category() {
        return category;
    }
}
