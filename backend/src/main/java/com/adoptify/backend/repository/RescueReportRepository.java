package com.adoptify.backend.repository;

import com.adoptify.backend.model.RescueReport;
import com.adoptify.backend.model.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RescueReportRepository extends JpaRepository<RescueReport, Long> {

    List<RescueReport> findByStatus(RescueStatus status);

    List<RescueReport> findByReporterId(Long reporterId);

    List<RescueReport> findByStatusIn(List<RescueStatus> statuses);

    @Query("SELECT r FROM RescueReport r WHERE r.status = 'PENDING' AND r.latitude BETWEEN :minLat AND :maxLat AND r.longitude BETWEEN :minLng AND :maxLng")
    List<RescueReport> findNearbyPendingReports(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);
}
