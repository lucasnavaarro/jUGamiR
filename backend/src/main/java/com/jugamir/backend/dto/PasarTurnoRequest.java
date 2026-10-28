package com.jugamir.backend.dto;
import jakarta.validation.constraints.NotNull;
public record PasarTurnoRequest(@NotNull Long preguntaPartidaId) {}
