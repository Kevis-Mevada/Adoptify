package com.adoptify.backend.config;

import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.AnimalRepository;
import com.adoptify.backend.repository.RescueReportRepository;
import com.adoptify.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private RescueReportRepository rescueReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (userRepository.count() > 0) {
            logger.info("Database already has data. Skipping initialization.");
            return;
        }

        logger.info("Initializing sample data...");

        // ===== ADMIN =====
        User admin = userRepository.save(User.builder()
                .email("admin@adoptify.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Admin User")
                .phone("9999999999")
                .role(UserRole.ADMIN)
                .isVerified(true)
                .isActive(true)
                .build());
        logger.info("Created admin user: {}", admin.getEmail());

        // ===== OWNER =====
        User owner1 = userRepository.save(User.builder()
                .email("owner1@adoptify.com")
                .password(passwordEncoder.encode("owner123"))
                .fullName("Rahul Sharma")
                .phone("9876543210")
                .address("123 MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .latitude(12.9716)
                .longitude(77.5946)
                .role(UserRole.REGULAR_USER)
                .isVerified(true)
                .isActive(true)
                .build());

        User owner2 = userRepository.save(User.builder()
                .email("owner2@adoptify.com")
                .password(passwordEncoder.encode("owner123"))
                .fullName("Priya Patel")
                .phone("9876543211")
                .address("456 Park Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .latitude(19.0760)
                .longitude(72.8777)
                .role(UserRole.REGULAR_USER)
                .isVerified(true)
                .isActive(true)
                .build());

        // ===== ADOPTER =====
        User adopter = userRepository.save(User.builder()
                .email("adopter1@adoptify.com")
                .password(passwordEncoder.encode("adopter123"))
                .fullName("Amit Kumar")
                .phone("9876543212")
                .address("789 Gandhi Nagar")
                .city("Delhi")
                .state("Delhi")
                .pincode("110001")
                .latitude(28.6139)
                .longitude(77.2090)
                .role(UserRole.REGULAR_USER)
                .isVerified(true)
                .isActive(true)
                .build());

        // ===== NGO =====
        User ngo = userRepository.save(User.builder()
                .email("ngo@animalrescue.org")
                .password(passwordEncoder.encode("ngo12345"))
                .fullName("Animal Rescue Foundation")
                .phone("9876543213")
                .address("101 NGO Lane")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560002")
                .latitude(12.9352)
                .longitude(77.6245)
                .role(UserRole.NGO)
                .organizationName("Animal Rescue Foundation")
                .licenseNumber("NGO-KA-2024-001")
                .isVerified(true)
                .rescueEnabled(true)
                .isActive(true)
                .build());

        // ===== RESCUER =====
        User rescuer = userRepository.save(User.builder()
                .email("rescuer@adoptify.com")
                .password(passwordEncoder.encode("rescue123"))
                .fullName("Vikram Singh")
                .phone("9876543214")
                .address("202 Rescue Street")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560003")
                .latitude(12.9500)
                .longitude(77.5800)
                .role(UserRole.REGULAR_USER)
                .isVerified(true)
                .isActive(true)
                .build());

        // ===== VETERINARIAN =====
        User vet = userRepository.save(User.builder()
                .email("vet@petcare.com")
                .password(passwordEncoder.encode("vet12345"))
                .fullName("Dr. Sneha Reddy")
                .phone("9876543215")
                .address("303 Vet Clinic Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560004")
                .latitude(12.9600)
                .longitude(77.6100)
                .role(UserRole.REGULAR_USER)
                .isVerified(true)
                .isActive(true)
                .build());

        logger.info("Created {} users", userRepository.count());

        // ===== ANIMALS =====
        animalRepository.save(Animal.builder()
                .owner(owner1)
                .name("Bruno")
                .species(Species.DOG)
                .breed("Labrador Retriever")
                .ageYears(2)
                .ageMonths(6)
                .gender(Gender.MALE)
                .size(AnimalSize.LARGE)
                .color("Golden")
                .description("Friendly and energetic Labrador. Loves to play fetch and is great with kids.")
                .healthStatus("Excellent")
                .vaccinated(true)
                .dewormed(true)
                .neutered(true)
                .goodWithKids(true)
                .goodWithPets(true)
                .adoptionFee(new BigDecimal("500"))
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build());

        animalRepository.save(Animal.builder()
                .owner(owner1)
                .name("Whiskers")
                .species(Species.CAT)
                .breed("Persian")
                .ageYears(1)
                .ageMonths(3)
                .gender(Gender.FEMALE)
                .size(AnimalSize.MEDIUM)
                .color("White")
                .description("Calm and affectionate Persian cat. Loves to be cuddled and nap in sunny spots.")
                .healthStatus("Good")
                .vaccinated(true)
                .dewormed(true)
                .neutered(false)
                .goodWithKids(true)
                .goodWithPets(false)
                .adoptionFee(new BigDecimal("300"))
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build());

        animalRepository.save(Animal.builder()
                .owner(owner2)
                .name("Rocky")
                .species(Species.DOG)
                .breed("German Shepherd")
                .ageYears(3)
                .ageMonths(0)
                .gender(Gender.MALE)
                .size(AnimalSize.LARGE)
                .color("Black & Tan")
                .description("Well-trained German Shepherd. Very protective and loyal. Needs experienced owner.")
                .healthStatus("Excellent")
                .vaccinated(true)
                .dewormed(true)
                .neutered(true)
                .specialNeeds("Needs daily exercise")
                .goodWithKids(true)
                .goodWithPets(false)
                .adoptionFee(new BigDecimal("800"))
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build());

        animalRepository.save(Animal.builder()
                .owner(owner2)
                .name("Tweety")
                .species(Species.BIRD)
                .breed("Budgerigar")
                .ageYears(0)
                .ageMonths(8)
                .gender(Gender.FEMALE)
                .size(AnimalSize.SMALL)
                .color("Yellow & Green")
                .description("Cheerful budgie that loves to chirp and sing. Easy to care for.")
                .healthStatus("Good")
                .vaccinated(false)
                .dewormed(false)
                .neutered(false)
                .goodWithKids(true)
                .goodWithPets(true)
                .adoptionFee(new BigDecimal("200"))
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build());

        animalRepository.save(Animal.builder()
                .owner(owner1)
                .name("Bunny")
                .species(Species.RABBIT)
                .breed("Holland Lop")
                .ageYears(1)
                .ageMonths(0)
                .gender(Gender.MALE)
                .size(AnimalSize.SMALL)
                .color("Brown & White")
                .description("Adorable Holland Lop rabbit. Very gentle and loves to hop around the garden.")
                .healthStatus("Good")
                .vaccinated(true)
                .dewormed(true)
                .neutered(false)
                .goodWithKids(true)
                .goodWithPets(true)
                .adoptionFee(new BigDecimal("250"))
                .status(AdoptionStatus.AVAILABLE)
                .viewsCount(0)
                .build());

        logger.info("Created {} animals", animalRepository.count());

        // ===== RESCUE REPORTS =====
        rescueReportRepository.save(RescueReport.builder()
                .reporter(adopter)
                .reporterName("Amit Kumar")
                .reporterPhone("9876543212")
                .animalType("Dog")
                .animalCount(1)
                .animalCondition(AnimalCondition.INJURED)
                .emergencyLevel(EmergencyLevel.URGENT)
                .locationAddress("Near Koramangala 4th Block, Bengaluru")
                .latitude(12.9340)
                .longitude(77.6266)
                .landmark("Next to the park entrance")
                .description("Injured stray dog spotted near the park. Looks like a leg injury. Needs immediate attention.")
                .status(RescueStatus.PENDING)
                .build());

        rescueReportRepository.save(RescueReport.builder()
                .reporter(owner1)
                .reporterName("Rahul Sharma")
                .reporterPhone("9876543210")
                .animalType("Cat")
                .animalCount(3)
                .animalCondition(AnimalCondition.SICK)
                .emergencyLevel(EmergencyLevel.NORMAL)
                .locationAddress("Indiranagar, Bengaluru")
                .latitude(12.9784)
                .longitude(77.6408)
                .landmark("Behind the metro station")
                .description("Three kittens found abandoned near a dumpster. They appear weak and might need medical care.")
                .status(RescueStatus.PENDING)
                .build());

        logger.info("Created {} rescue reports", rescueReportRepository.count());
        logger.info("Data initialization complete!");
    }
}
