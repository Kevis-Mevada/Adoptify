package com.adoptify.backend.service;

import com.adoptify.backend.dto.request.AnimalRequest;
import com.adoptify.backend.dto.response.AnimalDTO;
import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.AnimalRepository;
import com.adoptify.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnimalService {

    private static final Logger logger = LoggerFactory.getLogger(AnimalService.class);

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ===== DTO Methods =====

    @Transactional(readOnly = true)
    public Page<AnimalDTO> getAllAnimals(Species species, AdoptionStatus status, String search, String location,
            Pageable pageable) {
        if (species == null && status == null && search == null && location == null) {
            return animalRepository.findAll(pageable).map(this::convertToDTO);
        }
        return animalRepository.searchAnimals(status, species, search, location, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public AnimalDTO getAnimalDTOById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Animal not found with id: " + id));
        return convertToDTO(animal);
    }

    @Transactional(readOnly = true)
    public List<AnimalDTO> getAnimalsDTOByOwner(Long ownerId) {
        return animalRepository.findByOwnerId(ownerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalDTO> getAnimalsDTOBySpecies(Species species) {
        return animalRepository.findBySpeciesAndStatus(species, AdoptionStatus.AVAILABLE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalDTO> searchAnimalsDTO(Species species, AdoptionStatus status, String breed, AnimalSize size) {
        List<Animal> animals;
        if (species != null && status != null) {
            animals = animalRepository.findBySpeciesAndStatus(species, status);
        } else if (status != null) {
            animals = animalRepository.findByStatus(status);
        } else if (species != null) {
            animals = animalRepository.findAvailableBySpecies(species);
        } else {
            animals = animalRepository.findByStatus(AdoptionStatus.AVAILABLE);
        }
        return animals.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ===== Entity Methods =====

    @Transactional
    public AnimalDTO createAnimal(AnimalRequest request, User owner) {
        Animal animal = Animal.builder()
                .owner(owner)
                .name(request.getName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .ageYears(request.getAgeYears())
                .ageMonths(request.getAgeMonths())
                .gender(request.getGender())
                .size(request.getSize())
                .color(request.getColor())
                .description(request.getDescription())
                .healthStatus(request.getHealthStatus())
                .vaccinated(request.getVaccinated())
                .dewormed(request.getDewormed())
                .neutered(request.getNeutered())
                .specialNeeds(request.getSpecialNeeds())
                .behaviorNotes(request.getBehaviorNotes())
                .goodWithKids(request.getGoodWithKids())
                .goodWithPets(request.getGoodWithPets())
                .adoptionFee(request.getAdoptionFee())
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build();

        Animal saved = animalRepository.save(animal);
        return convertToDTO(saved);
    }

    @Transactional
    public AnimalDTO updateAnimal(Long id, AnimalRequest request, User user) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Animal not found with id: " + id));

        if (!animal.getOwner().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Not authorized to update this animal");
        }

        animal.setName(request.getName());
        animal.setSpecies(request.getSpecies());
        animal.setBreed(request.getBreed());
        animal.setAgeYears(request.getAgeYears());
        animal.setAgeMonths(request.getAgeMonths());
        animal.setGender(request.getGender());
        animal.setSize(request.getSize());
        animal.setColor(request.getColor());
        animal.setDescription(request.getDescription());
        animal.setHealthStatus(request.getHealthStatus());
        animal.setVaccinated(request.getVaccinated());
        animal.setDewormed(request.getDewormed());
        animal.setNeutered(request.getNeutered());
        animal.setSpecialNeeds(request.getSpecialNeeds());
        animal.setBehaviorNotes(request.getBehaviorNotes());
        animal.setGoodWithKids(request.getGoodWithKids());
        animal.setGoodWithPets(request.getGoodWithPets());
        animal.setAdoptionFee(request.getAdoptionFee());

        if (request.getStatus() != null) {
            animal.setStatus(request.getStatus());
        }

        Animal saved = animalRepository.save(animal);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteAnimal(Long id, User user) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Animal not found with id: " + id));

        if (!animal.getOwner().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Not authorized to delete this animal");
        }
        animalRepository.delete(animal);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        animalRepository.incrementViewsCount(id);
    }

    @Transactional
    public AnimalDTO uploadAnimalImage(Long animalId, MultipartFile file, User user) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new RuntimeException("Animal not found with id: " + animalId));

        if (!animal.getOwner().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Not authorized to modify this animal");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, "animals", animalId.toString());
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/animals/" + animalId + "/" + filename;

            boolean isPrimary = animal.getImages().isEmpty();
            AnimalImage image = AnimalImage.builder()
                    .animal(animal)
                    .imageUrl(imageUrl)
                    .isPrimary(isPrimary)
                    .build();

            animal.getImages().add(image);
            Animal saved = animalRepository.save(animal);

            logger.info("Image uploaded for animal {}: {}", animalId, imageUrl);
            return convertToDTO(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    // ===== DTO Converter =====

    private AnimalDTO convertToDTO(Animal animal) {
        AnimalDTO.OwnerInfoDTO ownerInfo = null;
        if (animal.getOwner() != null) {
            User owner = animal.getOwner();
            ownerInfo = AnimalDTO.OwnerInfoDTO.builder()
                    .id(owner.getId())
                    .fullName(owner.getFullName())
                    .email(owner.getEmail())
                    .phone(owner.getPhone())
                    .city(owner.getCity())
                    .build();
        }

        List<AnimalDTO.AnimalImageDTO> imageDTOs = Collections.emptyList();
        if (animal.getImages() != null) {
            imageDTOs = animal.getImages().stream()
                    .map(img -> AnimalDTO.AnimalImageDTO.builder()
                            .id(img.getId())
                            .imageUrl(img.getImageUrl())
                            .isPrimary(img.getIsPrimary())
                            .build())
                    .collect(Collectors.toList());
        }

        return AnimalDTO.builder()
                .id(animal.getId())
                .name(animal.getName())
                .species(animal.getSpecies() != null ? animal.getSpecies().name() : null)
                .breed(animal.getBreed())
                .ageYears(animal.getAgeYears())
                .ageMonths(animal.getAgeMonths())
                .gender(animal.getGender() != null ? animal.getGender().name() : null)
                .size(animal.getSize() != null ? animal.getSize().name() : null)
                .color(animal.getColor())
                .description(animal.getDescription())
                .healthStatus(animal.getHealthStatus())
                .vaccinated(animal.getVaccinated())
                .dewormed(animal.getDewormed())
                .neutered(animal.getNeutered())
                .specialNeeds(animal.getSpecialNeeds())
                .behaviorNotes(animal.getBehaviorNotes())
                .goodWithKids(animal.getGoodWithKids())
                .goodWithPets(animal.getGoodWithPets())
                .adoptionFee(animal.getAdoptionFee())
                .status(animal.getStatus() != null ? animal.getStatus().name() : null)
                .viewsCount(animal.getViewsCount())
                .images(imageDTOs)
                .owner(ownerInfo)
                .createdAt(animal.getCreatedAt())
                .updatedAt(animal.getUpdatedAt())
                .build();
    }
}
