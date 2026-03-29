package com.adoptify.backend.controller;

import com.adoptify.backend.dto.request.AnimalRequest;
import com.adoptify.backend.dto.response.AnimalDTO;
import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.UserRepository;
import com.adoptify.backend.security.services.UserDetailsImpl;
import com.adoptify.backend.service.AnimalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/animals")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnimalController {

    @Autowired
    private AnimalService animalService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<AnimalDTO>> getAllAnimals(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String direction,
            @RequestParam(value = "species", required = false) Species species,
            @RequestParam(value = "status", required = false) AdoptionStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "location", required = false) String location) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(animalService.getAllAnimals(species, status, search, location, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AnimalDTO> getAnimalById(@PathVariable("id") Long id) {
        animalService.incrementViewCount(id);
        return ResponseEntity.ok(animalService.getAnimalDTOById(id));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AnimalDTO>> getAnimalsByOwner(@PathVariable("ownerId") Long ownerId) {
        return ResponseEntity.ok(animalService.getAnimalsDTOByOwner(ownerId));
    }

    @GetMapping("/my-animals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AnimalDTO>> getMyAnimals(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(animalService.getAnimalsDTOByOwner(userDetails.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnimalDTO> addAnimal(@Valid @RequestBody AnimalRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User owner = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(animalService.createAnimal(request, owner));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnimalDTO> updateAnimal(@PathVariable("id") Long id,
            @Valid @RequestBody AnimalRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        return ResponseEntity.ok(animalService.updateAnimal(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAnimal(@PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        animalService.deleteAnimal(id, user);
        return ResponseEntity.ok(Map.of("message", "Animal deleted successfully"));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnimalDTO> uploadAnimalImage(@PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        return ResponseEntity.ok(animalService.uploadAnimalImage(id, file, user));
    }

    @GetMapping("/species/{species}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<AnimalDTO>> getAnimalsBySpecies(@PathVariable("species") Species species) {
        return ResponseEntity.ok(animalService.getAnimalsDTOBySpecies(species));
    }

    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<AnimalDTO>> searchAnimals(
            @RequestParam(value = "species", required = false) Species species,
            @RequestParam(value = "status", required = false) AdoptionStatus status,
            @RequestParam(value = "breed", required = false) String breed,
            @RequestParam(value = "size", required = false) AnimalSize size) {
        return ResponseEntity.ok(animalService.searchAnimalsDTO(species, status, breed, size));
    }
}
