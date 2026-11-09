package com.jugamir.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterJugadorRequest extends RegisterRequest {

    @NotBlank @jakarta.validation.constraints.Size(max=80)
    private String nick;

    public void setNick(String nick) { this.nick = nick == null ? null : nick.strip(); }
}
