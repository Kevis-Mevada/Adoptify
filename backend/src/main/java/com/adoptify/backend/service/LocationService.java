package com.adoptify.backend.service;

import com.adoptify.backend.model.User;
import com.adoptify.backend.model.UserRole;
import com.adoptify.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Autowired
    private UserRepository userRepository;

    public List<User> findNearbyRescuers(Double latitude, Double longitude, Double radiusInKm) {
        // Calculate bounding box for initial DB filter (faster than checking all users)
        double latDelta = radiusInKm / EARTH_RADIUS_KM * (180.0 / Math.PI);
        double lngDelta = radiusInKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(latitude))) * (180.0 / Math.PI);

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLng = longitude - lngDelta;
        double maxLng = longitude + lngDelta;

        // Get users within bounding box
        List<User> candidates = userRepository
                .findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng);

        return candidates.stream()
                .filter(user -> user.getRole() == UserRole.NGO)
                .filter(user -> user.getIsVerified() != null && user.getIsVerified())
                .filter(user -> user.getRescueEnabled() != null && user.getRescueEnabled())
                .filter(user -> user.getIsActive() != null && user.getIsActive())
                .filter(user -> user.getLatitude() != null && user.getLongitude() != null)
                .filter(user -> calculateDistance(latitude, longitude,
                        user.getLatitude(), user.getLongitude()) <= radiusInKm)
                .collect(Collectors.toList());
    }

    /**
     * Calculate distance between two coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
