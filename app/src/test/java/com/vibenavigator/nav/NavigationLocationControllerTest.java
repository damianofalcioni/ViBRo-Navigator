package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.location.Location;
import android.location.LocationManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NavigationLocationControllerTest {

    @Test
    public void isUsableStartupLastKnownLocation_rejectsStaleFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.GPS_PROVIDER, nowMs - 16_000L, 8f);

        assertFalse(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

    @Test
    public void isUsableStartupLastKnownLocation_rejectsLowQualityFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.NETWORK_PROVIDER, nowMs - 2_000L, 75f);

        assertFalse(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

    @Test
    public void selectBestStartupLastKnownLocation_prefersFreshUsableFix() {
        long nowMs = 100_000L;
        Location staleGps = location(LocationManager.GPS_PROVIDER, nowMs - 30_000L, 5f);
        Location freshNetwork = location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 20f);

        Location selected =
                NavigationLocationController.selectBestStartupLastKnownLocation(staleGps, freshNetwork, nowMs);

        assertNotNull(selected);
        assertEquals(LocationManager.NETWORK_PROVIDER, selected.getProvider());
    }

    @Test
    public void selectBestStartupLastKnownLocation_returnsNullWhenNoUsableFixExists() {
        long nowMs = 100_000L;
        Location staleGps = location(LocationManager.GPS_PROVIDER, nowMs - 20_000L, 5f);
        Location inaccurateNetwork = location(LocationManager.NETWORK_PROVIDER, nowMs - 1_000L, 120f);

        Location selected =
                NavigationLocationController.selectBestStartupLastKnownLocation(staleGps, inaccurateNetwork, nowMs);

        assertNull(selected);
    }

    @Test
    public void isUsableStartupLastKnownLocation_acceptsFreshAccurateFix() {
        long nowMs = 100_000L;
        Location location = location(LocationManager.GPS_PROVIDER, nowMs - 5_000L, 12f);

        assertTrue(NavigationLocationController.isUsableStartupLastKnownLocation(location, nowMs));
    }

    private static Location location(String provider, long timeMs, float accuracyMeters) {
        Location location = new Location(provider);
        location.setLatitude(48.2082d);
        location.setLongitude(16.3738d);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }
}
