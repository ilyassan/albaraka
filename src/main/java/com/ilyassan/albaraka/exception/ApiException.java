package com.ilyassan.albaraka.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ApiException {
    private String message;
    private HttpStatus status;
    private int statusCode;
    private LocalDateTime timestamp;
    private String path;
    private String error;
}
