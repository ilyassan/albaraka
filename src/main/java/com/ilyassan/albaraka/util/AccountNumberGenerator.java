package com.ilyassan.albaraka.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AccountNumberGenerator {

    public String generateAccountNumber() {
        // Format: ALBARAKA + timestamp + random suffix
        // Example: ALBARAKA202512171630459a7b8c9d
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ALBARAKA" + timestamp + randomSuffix;
    }
}
