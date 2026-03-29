package com.adoptify.backend.service;

import com.adoptify.backend.dto.request.AdoptionRequestDTO;
import com.adoptify.backend.dto.response.AdoptionResponseDTO;
import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.AdoptionRequestRepository;
import com.adoptify.backend.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdoptionService {

    @Autowired
    private AdoptionRequestRepository adoptionRequestRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public AdoptionResponseDTO createAdoptionRequest(AdoptionRequestDTO request, User adopter) {
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new RuntimeException("Animal not found"));

        if (animal.getStatus() != AdoptionStatus.AVAILABLE) {
            throw new RuntimeException("Animal is not available for adoption");
        }

        if (animal.getOwner().getId().equals(adopter.getId())) {
            throw new RuntimeException("You cannot adopt your own animal");
        }

        if (checkIfAlreadyRequested(request.getAnimalId(), adopter.getId())) {
            throw new RuntimeException("You already have a pending or approved request for this animal");
        }

        AdoptionRequest adoptionRequest = AdoptionRequest.builder()
                .animal(animal)
                .adopter(adopter)
                .owner(animal.getOwner())
                .requestStatus(RequestStatus.PENDING)
                .hasExperience(request.getHasExperience())
                .hasOtherPets(request.getHasOtherPets())
                .hasYard(request.getHasYard())
                .livingSituation(request.getLivingSituation() != null ? LivingSituation.valueOf(request.getLivingSituation().toUpperCase()) : null)
                .dailyHoursAlone(request.getDailyHoursAlone())
                .reasonForAdoption(request.getReasonForAdoption())
                .additionalNotes(request.getAdditionalNotes())
                .adopterContactPhone(request.getAdopterContactPhone())
                .adopterContactEmail(request.getAdopterContactEmail())
                .preferredMeetingDate(request.getPreferredMeetingDate())
                .meetingAddress(request.getMeetingAddress())
                .viewedByOwner(false)
                .build();

        AdoptionRequest saved = adoptionRequestRepository.save(adoptionRequest);

        // Send notification email to owner
        emailService.sendAdoptionRequestEmailToOwner(saved);

        return convertToResponseDTO(saved, adopter.getId());
    }

    public List<AdoptionResponseDTO> getRequestsForOwner(Long ownerId, RequestStatus status) {
        List<AdoptionRequest> requests;
        if (status == null) {
            requests = adoptionRequestRepository.findByAnimal_OwnerIdAndRequestStatus(ownerId, RequestStatus.PENDING);
            // Also include approved ones for management
            requests.addAll(adoptionRequestRepository.findByAnimal_OwnerIdAndRequestStatus(ownerId, RequestStatus.APPROVED));
        } else {
            requests = adoptionRequestRepository.findByAnimal_OwnerIdAndRequestStatus(ownerId, status);
        }
        return requests.stream()
                .map(req -> convertToResponseDTO(req, ownerId))
                .collect(Collectors.toList());
    }

    public List<AdoptionResponseDTO> getMyRequests(Long adopterId, RequestStatus status) {
        List<AdoptionRequest> requests;
        if (status == null) {
            requests = adoptionRequestRepository.findByAdopterId(adopterId);
        } else {
            requests = adoptionRequestRepository.findByAdopterIdAndRequestStatus(adopterId, status);
        }
        return requests.stream()
                .map(req -> convertToResponseDTO(req, adopterId))
                .collect(Collectors.toList());
    }

    @Transactional
    public AdoptionResponseDTO approveRequest(Long requestId, User user) {
        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Adoption request not found"));

        if (!request.getAnimal().getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the owner can approve this request");
        }

        if (request.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be approved");
        }

        request.setRequestStatus(RequestStatus.APPROVED);

        // Update animal status to PENDING
        Animal animal = request.getAnimal();
        animal.setStatus(AdoptionStatus.PENDING);
        animalRepository.save(animal);

        AdoptionRequest saved = adoptionRequestRepository.save(request);

        // Notify adopter of approval with owner contact info
        emailService.sendAdoptionApprovedEmailToAdopter(saved);

        return convertToResponseDTO(saved, user.getId());
    }

    @Transactional
    public AdoptionResponseDTO rejectRequest(Long requestId, String rejectionReason, User user) {
        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Adoption request not found"));

        if (!request.getAnimal().getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the owner can reject this request");
        }

        if (request.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be rejected");
        }

        request.setRequestStatus(RequestStatus.REJECTED);
        request.setRejectionReason(rejectionReason);
        AdoptionRequest saved = adoptionRequestRepository.save(request);

        // Notify adopter of rejection
        emailService.sendAdoptionRejectedEmailToAdopter(saved);

        return convertToResponseDTO(saved, user.getId());
    }

    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Adoption request not found"));

        if (!request.getAdopter().getId().equals(userId)) {
            throw new RuntimeException("Only the adopter can cancel this request");
        }

        if (request.getRequestStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }

        request.setRequestStatus(RequestStatus.CANCELLED);
        adoptionRequestRepository.save(request);
    }

    @Transactional
    public void completeAdoption(Long requestId, Long userId) {
        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Adoption request not found"));

        if (!request.getAnimal().getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the owner can mark this as completed");
        }

        if (request.getRequestStatus() != RequestStatus.APPROVED) {
            throw new RuntimeException("Only approved requests can be marked as completed");
        }

        request.setRequestStatus(RequestStatus.COMPLETED);
        
        Animal animal = request.getAnimal();
        animal.setStatus(AdoptionStatus.ADOPTED);
        animalRepository.save(animal);
        
        adoptionRequestRepository.save(request);
    }

    public boolean checkIfAlreadyRequested(Long animalId, Long adopterId) {
        return adoptionRequestRepository
                .findByAnimalIdAndAdopterIdAndRequestStatus(animalId, adopterId, RequestStatus.PENDING)
                .isPresent() || 
               adoptionRequestRepository
                .findByAnimalIdAndAdopterIdAndRequestStatus(animalId, adopterId, RequestStatus.APPROVED)
                .isPresent();
    }

    public AdoptionResponseDTO convertToResponseDTO(AdoptionRequest request, Long currentUserId) {
        RequestStatus status = request.getRequestStatus();
        boolean isAdopter = request.getAdopter().getId().equals(currentUserId);
        boolean isApproved = status == RequestStatus.APPROVED;
        
        AdoptionResponseDTO dto = AdoptionResponseDTO.builder()
                .id(request.getId())
                .animalId(request.getAnimal().getId())
                .animalName(request.getAnimal().getName())
                .animalImage(!request.getAnimal().getImages().isEmpty() ? request.getAnimal().getImages().get(0).getImageUrl() : null)
                .animalBreed(request.getAnimal().getBreed())
                .animalAge((request.getAnimal().getAgeYears() != null ? request.getAnimal().getAgeYears() + "Y " : "") + 
                           (request.getAnimal().getAgeMonths() != null ? request.getAnimal().getAgeMonths() + "M" : ""))
                .animalGender(request.getAnimal().getGender() != null ? request.getAnimal().getGender().name() : null)
                .animalSize(request.getAnimal().getSize() != null ? request.getAnimal().getSize().name() : null)
                .animalColor(request.getAnimal().getColor())
                .adopterId(request.getAdopter().getId())
                .adopterName(request.getAdopter().getFullName())
                .ownerId(request.getAnimal().getOwner().getId())
                .ownerName(request.getAnimal().getOwner().getFullName())
                .requestStatus(request.getRequestStatus().name())
                .reasonForAdoption(request.getReasonForAdoption())
                .livingSituation(request.getLivingSituation() != null ? request.getLivingSituation().name() : null)
                .hasYard(request.getHasYard())
                .hasOtherPets(request.getHasOtherPets())
                .hasExperience(request.getHasExperience())
                .dailyHoursAlone(request.getDailyHoursAlone())
                .preferredMeetingDate(request.getPreferredMeetingDate())
                .meetingAddress(request.getMeetingAddress())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .build();

        // Contact details security rules
        if (isAdopter) {
            // I am the adopter. Show owner details ONLY if approved.
            if (isApproved || status == RequestStatus.COMPLETED) {
                dto.setOwnerPhone(request.getAnimal().getOwner().getPhone());
                dto.setOwnerEmail(request.getAnimal().getOwner().getEmail());
                dto.setOwnerAddress(request.getAnimal().getOwner().getAddress());
            } else {
                dto.setOwnerPhone(null);
                dto.setOwnerEmail(null);
                dto.setOwnerAddress(null);
            }
            // Always show my own contact info provided in the request
            dto.setAdopterPhone(request.getAdopterContactPhone());
            dto.setAdopterEmail(request.getAdopterContactEmail());
        } else {
            // I am the owner. Always show adopter details so I can contact them.
            dto.setAdopterPhone(request.getAdopterContactPhone());
            dto.setAdopterEmail(request.getAdopterContactEmail());
            // No need to show my own contact info back to me.
            dto.setOwnerPhone(null);
            dto.setOwnerEmail(null);
            dto.setOwnerAddress(null);
        }

        return dto;
    }
}
