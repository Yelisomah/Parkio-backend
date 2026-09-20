package com.example.parkio.dto.response;

/** Wraps a lot with its distance from the search point, in kilometers. */
public record NearbyLotResponse(
        ParkingLotResponse lot,
        double distanceKm
) {}
