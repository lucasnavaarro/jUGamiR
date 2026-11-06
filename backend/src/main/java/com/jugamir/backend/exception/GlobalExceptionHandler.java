package com.jugamir.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            jakarta.validation.ConstraintViolationException.class, java.util.zip.ZipException.class})
    public ResponseEntity<Map<String, String>> handleBadInput(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Revisa los campos y el formato de la petición"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethod(Exception ex) {
        return ResponseEntity.status(405).body(Map.of("error", "Método no permitido"));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMedia(Exception ex) {
        return ResponseEntity.status(415).body(Map.of("error", "Formato de contenido no admitido"));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUpload(Exception ex) {
        return ResponseEntity.status(413).body(Map.of("error", "El archivo supera el tamaño permitido"));
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class, java.util.NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(Exception ex) {
        return ResponseEntity.status(404).body(Map.of("error", "Recurso no encontrado"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("error", "Los datos ya existen o están referenciados por otros registros"));
    }

    @ExceptionHandler(org.springframework.mail.MailException.class)
    public ResponseEntity<Map<String, String>> handleMail(Exception ex) {
        return ResponseEntity.status(503).body(Map.of("error", "No se pudo enviar el correo. Inténtalo de nuevo más tarde"));
    }

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<Map<String, String>> handleStorage(Exception ex) {
        return ResponseEntity.status(503).body(Map.of("error", "No se pudo acceder al almacenamiento. Inténtalo de nuevo más tarde"));
    }

    @ExceptionHandler(javax.imageio.IIOException.class)
    public ResponseEntity<Map<String, String>> handleImage(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "El archivo no es una imagen válida"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(Exception ex) {
        return ResponseEntity.status(403).body(Map.of("error", "No tienes permiso para esta operación"));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(Exception ex) {
        return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Operación rechazada" : ex.getReason()));
    }

    // Errores de negocio esperados: email duplicado, código incorrecto...
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // Errores de validación (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", mensaje));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() == null ? "Entrada inválida" : e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    // Errores inesperados: no exponemos detalles al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
