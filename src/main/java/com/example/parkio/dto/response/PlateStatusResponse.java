package com.example.parkio.dto.response;

import java.util.List;

public record PlateStatusResponse(
        String plateNumber,
        List<BookingResponse> activeBookings,
        List<FineResponse> unpaidFines
) {}
