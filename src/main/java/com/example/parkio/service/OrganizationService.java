package com.example.parkio.service;

import com.example.parkio.dto.request.OrganizationRequest;
import com.example.parkio.dto.response.OrganizationResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.Organization;
import com.example.parkio.entity.Staff;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.OrganizationRepository;
import com.example.parkio.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final StaffRepository staffRepository;
    private final UserService userService;
    private final AuditService auditService;

    /**
     * Creates the organization and auto-assigns the creator as ORG_ADMIN
     * (tasks.md 3.2) — this is how every organization gets its first staff
     * member, since there's no other bootstrap path.
     */
    @Transactional
    public OrganizationResponse create(Long creatorUserId, OrganizationRequest request) {
        User creator = userService.findById(creatorUserId);

        Organization org = Organization.builder()
                .owner(creator)
                .name(request.name())
                .contactPhone(request.contactPhone())
                .build();
        Organization saved = organizationRepository.save(org);

        Staff orgAdmin = Staff.builder()
                .organization(saved)
                .user(creator)
                .role(Staff.StaffRole.ORG_ADMIN)
                .build();
        staffRepository.save(orgAdmin);

        auditService.log(creator.getEmail(), AuditLog.AuditAction.ORGANIZATION_CREATED,
                "Organization", saved.getId(), "Organization \"" + saved.getName() + "\" created, creator auto-assigned as ORG_ADMIN");

        return OrganizationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(Long id) {
        return OrganizationResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getMine(Long userId) {
        return organizationRepository.findByOwnerId(userId).stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationResponse update(Long id, Long actingUserId, OrganizationRequest request) {
        Organization org = findById(id);
        // Only the owner can rename/re-contact the org itself (distinct from staff
        // role management, which StaffService gates on ORG_ADMIN membership).
        if (!org.getOwner().getId().equals(actingUserId)) {
            throw ParkioException.forbidden("Only the organization owner can update it");
        }
        org.setName(request.name());
        org.setContactPhone(request.contactPhone());
        Organization saved = organizationRepository.save(org);

        auditService.log(org.getOwner().getEmail(), AuditLog.AuditAction.ORGANIZATION_UPDATED,
                "Organization", saved.getId(), "Organization \"" + saved.getName() + "\" updated");

        return OrganizationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Organization findById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Organization not found"));
    }
}
