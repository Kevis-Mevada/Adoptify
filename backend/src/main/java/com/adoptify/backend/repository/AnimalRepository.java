package com.adoptify.backend.repository;

import com.adoptify.backend.model.AdoptionStatus;
import com.adoptify.backend.model.Animal;
import com.adoptify.backend.model.Species;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByOwnerId(Long ownerId);

    List<Animal> findByStatus(AdoptionStatus status);

    List<Animal> findBySpeciesAndStatus(Species species, AdoptionStatus status);

    List<Animal> findByOwnerIdAndStatus(Long ownerId, AdoptionStatus status);

    @Query("SELECT a FROM Animal a WHERE a.status = 'AVAILABLE' AND a.species = :species")
    List<Animal> findAvailableBySpecies(@Param("species") Species species);

    Page<Animal> findAll(Pageable pageable);

    @Query("SELECT a FROM Animal a " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND (:species IS NULL OR a.species = :species) " +
           "AND (:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR LOWER(a.breed) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:location IS NULL OR LOWER(a.owner.city) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<Animal> searchAnimals(
            @Param("status") AdoptionStatus status,
            @Param("species") Species species,
            @Param("search") String search,
            @Param("location") String location,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Animal a SET a.viewsCount = a.viewsCount + 1 WHERE a.id = :animalId")
    void incrementViewsCount(@Param("animalId") Long animalId);
}
