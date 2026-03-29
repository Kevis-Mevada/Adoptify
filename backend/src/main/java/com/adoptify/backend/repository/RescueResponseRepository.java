package com.adoptify.backend.repository;

import com.adoptify.backend.model.RescueResponse;
import com.adoptify.backend.model.ResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RescueResponseRepository extends JpaRepository<RescueResponse, Long> {

    List<RescueResponse> findByRescueReportId(Long rescueReportId);

    List<RescueResponse> findByResponderId(Long responderId);

    Optional<RescueResponse> findByRescueReportIdAndResponderId(Long rescueReportId, Long responderId);

    List<RescueResponse> findByRescueReportIdAndResponseStatus(Long rescueReportId, ResponseStatus status);
}
