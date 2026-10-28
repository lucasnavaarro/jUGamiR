package com.jugamir.backend.security;

import com.jugamir.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompSecurity implements ChannelInterceptor {
    private final JwtService jwt;
    private final UsuarioRepository usuarios;
    private final GameAccess access;
    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private static final Pattern ROOM = Pattern.compile("^/topic/(?:lobby|juego)/([1-9][0-9]*)$");

    Long authenticate(String token) {
        if (token == null) throw new AccessDeniedException("Debes iniciar sesión");
        try {
            var user = usuarios.findByEmail(jwt.getEmailFromToken(token)).orElseThrow();
            if (!Boolean.TRUE.equals(user.getEsActivo()) || user.getTokenVersion() != jwt.getTokenVersion(token))
                throw new AccessDeniedException("Sesión caducada");
            return user.getIdUsuario();
        } catch (Exception ex) {
            throw new AccessDeniedException("Sesión inválida o caducada");
        }
    }

    void authorize(String destination, Long userId) {
        if ("/topic/partidas-publicas".equals(destination)) return;
        var match = ROOM.matcher(destination == null ? "" : destination);
        if (!match.matches()) throw new AccessDeniedException("Destino no permitido");
        try { access.requireMember(Long.valueOf(match.group(1)), userId); }
        catch (NumberFormatException ex) { throw new AccessDeniedException("Destino no permitido"); }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) throw new AccessDeniedException("Trama no permitida");
        var command = headers.getCommand();
        String session = headers.getSessionId();
        if (command == StompCommand.DISCONNECT) { tokens.remove(session); return message; }
        if (command == StompCommand.CONNECT) {
            String bearer = headers.getFirstNativeHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) throw new AccessDeniedException("Debes iniciar sesión");
            String token = bearer.substring(7);
            Long userId = authenticate(token);
            headers.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
            tokens.put(session, token);
            return message;
        }
        Long userId = authenticate(tokens.get(session));
        if (command == StompCommand.SUBSCRIBE) authorize(headers.getDestination(), userId);
        else if (command != StompCommand.UNSUBSCRIBE && headers.getMessageType() != org.springframework.messaging.simp.SimpMessageType.HEARTBEAT)
            throw new AccessDeniedException("El cliente no puede publicar mensajes");
        return message;
    }

    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var h = StompHeaderAccessor.wrap(message);
                if (h.getMessageType() != org.springframework.messaging.simp.SimpMessageType.MESSAGE) return message;
                try {
                    authorize(h.getDestination(), authenticate(tokens.get(h.getSessionId())));
                    return message;
                } catch (AccessDeniedException ex) {
                    return null; // Also revoke already subscribed sockets after logout, reset or expulsion.
                }
            }
        };
    }

    @EventListener public void disconnected(SessionDisconnectEvent event) { tokens.remove(event.getSessionId()); }
}
