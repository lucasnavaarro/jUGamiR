package com.jugamir.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterProfesorRequest extends RegisterRequest {

    @NotBlank @jakarta.validation.constraints.Size(max=120)
    private String departamento;

}
