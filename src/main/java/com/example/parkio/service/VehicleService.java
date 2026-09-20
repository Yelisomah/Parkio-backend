package com.example.parkio.service;

import com.example.parkio.dto.request.VehicleRequest;
import com.example.parkio.dto.response.VehicleResponse;
import com.example.parkio.entity.User;
import com.example.parkio.entity.Vehicle;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<VehicleResponse> getByOwner(Long ownerId) {
        return vehicleRepository.findByOwnerId(ownerId)
                .stream().map(VehicleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(Long id, Long ownerId) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ParkioException.notFound("Vehicle not found: " + id));
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public VehicleResponse create(Long ownerId, VehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw ParkioException.conflict("License plate already registered: " + request.licensePlate());
        }

        User owner = userService.findById(ownerId);

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.licensePlate().toUpperCase())
                .make(request.make())
                .model(request.model())
                .year(request.year())
                .color(request.color())
                .type(request.type())
                .owner(owner)
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(Long id, Long ownerId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ParkioException.notFound("Vehicle not found: " + id));

        // Allow updating plate only if it changed and isn't taken
        if (!vehicle.getLicensePlate().equalsIgnoreCase(request.licensePlate())
                && vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw ParkioException.conflict("License plate already registered: " + request.licensePlate());
        }

        vehicle.setLicensePlate(request.licensePlate().toUpperCase());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setColor(request.color());
        vehicle.setType(request.type());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ParkioException.notFound("Vehicle not found: " + id));
        vehicleRepository.delete(vehicle);
    }

    public Vehicle findByIdRaw(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Vehicle not found: " + id));
    }

    @Transactional
    public Vehicle findOrCreateByPlate(String plate, Vehicle.VehicleType type, User owner) {
        return vehicleRepository.findByLicensePlate(plate.toUpperCase()).orElseGet(() ->
                vehicleRepository.save(Vehicle.builder()
                        .licensePlate(plate.toUpperCase())
                    .make("Unknown")
                    .model("Unknown")
                    .year("Unknown")
                        .type(type == null ? Vehicle.VehicleType.SEDAN : type)
                        .owner(owner)
                        .build()));
    }
}
