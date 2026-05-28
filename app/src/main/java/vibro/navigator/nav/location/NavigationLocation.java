package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;

public final class NavigationLocation {
    @Nullable
    private String provider;
    private long time;
    private double latitude;
    private double longitude;
    private double altitude;
    private boolean hasAltitude;
    private float accuracy;
    private boolean hasAccuracy;
    private float speed;
    private boolean hasSpeed;
    private float bearing;
    private boolean hasBearing;
    private float bearingAccuracyDegrees;
    private boolean hasBearingAccuracy;

    public NavigationLocation(@Nullable String provider) {
        this.provider = provider;
    }

    public NavigationLocation(@NonNull NavigationLocation source) {
        provider = source.provider;
        time = source.time;
        latitude = source.latitude;
        longitude = source.longitude;
        altitude = source.altitude;
        hasAltitude = source.hasAltitude;
        accuracy = source.accuracy;
        hasAccuracy = source.hasAccuracy;
        speed = source.speed;
        hasSpeed = source.hasSpeed;
        bearing = source.bearing;
        hasBearing = source.hasBearing;
        bearingAccuracyDegrees = source.bearingAccuracyDegrees;
        hasBearingAccuracy = source.hasBearingAccuracy;
    }

    @Nullable
    public String getProvider() {
        return provider;
    }

    public void setProvider(@Nullable String provider) {
        this.provider = provider;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasAltitude() {
        return hasAltitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
        hasAltitude = true;
    }

    public boolean hasAccuracy() {
        return hasAccuracy;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
        hasAccuracy = true;
    }

    public boolean hasSpeed() {
        return hasSpeed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        hasSpeed = true;
    }

    public boolean hasBearing() {
        return hasBearing;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
        hasBearing = true;
    }

    public boolean hasBearingAccuracy() {
        return hasBearingAccuracy;
    }

    public float getBearingAccuracyDegrees() {
        return bearingAccuracyDegrees;
    }

    public void setBearingAccuracyDegrees(float bearingAccuracyDegrees) {
        this.bearingAccuracyDegrees = bearingAccuracyDegrees;
        hasBearingAccuracy = true;
    }

    public float distanceTo(@NonNull NavigationLocation destination) {
        return (float) GeoMath.distanceMeters(latitude, longitude, destination.latitude, destination.longitude);
    }

    public float bearingTo(@NonNull NavigationLocation destination) {
        return (float) GeoMath.bearingDegrees(latitude, longitude, destination.latitude, destination.longitude);
    }
}
