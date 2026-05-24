package com.jugamir.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${app.url}")
    private String appUrl;

    public void enviarCodigo(String email, String codigo) {

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Código de verificación - jUGamiR");
        mensaje.setText("Tu código de verificación para acceder a jUGamiR es: " + codigo
                + "\n\n Este código expirará en 10 minutos y ya no será válido");
        mailSender.send(mensaje);

    }

    public void enviarEnlaceCambioContrasena(String email, String token) {

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Reestablecer contraseña - jUGamiR");
        mensaje.setText(
                "¿Mala memoria? \n\n " +
                        "Has solicitado un cambio de contraseña. Para reestablecerla, haz clic en el siguiente enlace: \n\n"
                        +
                        appUrl + "/reset-password?token=" + token + "\n\n" +
                        "Si no lo has solicitado, ignora este correo."

        );

        mailSender.send(mensaje);
    }

}
