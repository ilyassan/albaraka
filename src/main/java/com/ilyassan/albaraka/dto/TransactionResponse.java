package com.ilyassan.albaraka.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String status;
    private String justificationPath;
    private Long beneficiaryAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
