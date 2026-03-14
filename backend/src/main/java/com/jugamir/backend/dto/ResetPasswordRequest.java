package com.jugamir.backend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String token;
    private String nuevaPassword;
}
