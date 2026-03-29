package com.adoptify.backend.repository;

import com.adoptify.backend.model.AdoptionRequest;
import com.adoptify.backend.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, Long> {

    List<AdoptionRequest> findByOwnerId(Long ownerId);

    List<AdoptionRequest> findByAdopterId(Long adopterId);

    List<AdoptionRequest> findByAnimalId(Long animalId);

    List<AdoptionRequest> findByAnimal_OwnerIdAndRequestStatus(Long ownerId, RequestStatus status);

    List<AdoptionRequest> findByAdopterIdAndRequestStatus(Long adopterId, RequestStatus status);

    Optional<AdoptionRequest> findByAnimalIdAndAdopterIdAndRequestStatus(Long animalId, Long adopterId,
            RequestStatus status);

    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE AdoptionRequest a SET a.viewedByOwner = true WHERE a.id = :requestId")
    void markAsViewed(@Param("requestId") Long requestId);

    @Query("SELECT COUNT(a) FROM AdoptionRequest a WHERE a.owner.id = :ownerId AND a.requestStatus = 'PENDING'")
    Long countPendingRequestsByOwnerId(@Param("ownerId") Long ownerId);
}
