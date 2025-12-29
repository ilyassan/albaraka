package com.ilyassan.albaraka.dto;

import com.ilyassan.albaraka.entity.AIRecommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationResult {

    private AIRecommendation recommendation;
    private String reasoning;
    private Double confidenceScore;
    private Boolean isSuspicious;
    private String extractedText;
}
