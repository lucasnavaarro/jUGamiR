package com.jugamir.backend.dto;

import java.util.List;

public record ImportarResultadoDTO(
        int añadidas,
        int yaExistentes,
        int errores,
        List<String> detalleErrores) {

}
