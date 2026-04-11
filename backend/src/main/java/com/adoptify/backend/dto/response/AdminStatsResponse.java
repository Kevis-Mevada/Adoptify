package com.adoptify.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalNGOs;
    private long pendingNGOs;
    private long totalAnimals;
    private long totalRescues;
    private long totalAdoptions;
}
