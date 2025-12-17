package com.ilyassan.albaraka.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
