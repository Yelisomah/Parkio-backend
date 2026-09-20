package com.example.parkio.mapper;

import com.example.parkio.dto.response.ParkingSpotResponse;
import com.example.parkio.entity.ParkingSpot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


/**
 * Foundation for the tech-standards.md requirement ("DTOs are mapped from
 * entities via MapStruct. Controllers never return JPA entities directly.")
 *
 * The "never return JPA entities directly" part is already true throughout
 * this codebase — every controller returns a hand-written *Response record
 * via a static from() method. This mapper is the first MapStruct-based
 * replacement for one of those (ParkingSpotResponse.from(ParkingSpot)),
 * kept SEPARATE from ParkingSpotService for now rather than swapped in:
 * MapStruct generates its implementation at annotation-processing time, and
 * this session has no working compiler to verify the generated code once
 * (see STATUS_AND_ROADMAP.md). Swap ParkingSpotService's calls from
 * ParkingSpotResponse.from(spot) to parkingSpotMapper.toResponse(spot) once
 * you've built this once locally and confirmed the generated mapper matches
 * the hand-written version's behavior — then do the same for the rest of the
 * *Response.from() methods one at a time.
 */
@Mapper(componentModel = "spring")
public interface ParkingSpotMapper {

    @Mapping(target = "parkingLotId", source = "parkingLot.id")
    @Mapping(target = "parkingLotName", source = "parkingLot.name")
    // type/status are enums mapped to String — MapStruct does this via
    // Enum.name() automatically when the target field type is String.
    ParkingSpotResponse toResponse(ParkingSpot spot);
}
