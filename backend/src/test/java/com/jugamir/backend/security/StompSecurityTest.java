package com.jugamir.backend.security;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class StompSecurityTest {
    @Test void requiresAuthenticationAndAuthorizesEverySubscriptionAndDelivery() {
        var jwt = mock(JwtService.class); var users = mock(UsuarioRepository.class); var access = mock(GameAccess.class);
        var policy = new StompSecurity(jwt, users, access);
        var user = Usuario.builder().idUsuario(1L).esActivo(true).tokenVersion(1).build();
        when(jwt.getEmailFromToken("valid")).thenReturn("test@example.invalid");
        when(jwt.getTokenVersion("valid")).thenReturn(1);
        when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(user));
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT); connect.setSessionId("session"); connect.setLeaveMutable(true);
        var frame = MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders());
        assertThrows(AccessDeniedException.class, () -> policy.preSend(frame, null));
        connect.setNativeHeader("Authorization", "Bearer valid"); policy.preSend(frame, null);
        var sub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE); sub.setSessionId("session"); sub.setDestination("/topic/lobby/2");
        policy.preSend(MessageBuilder.createMessage(new byte[0], sub.getMessageHeaders()), null);
        verify(access).requireMember(2L, 1L);
        assertThrows(AccessDeniedException.class, () -> policy.authorize("/topic/lobby/*", 1L));
        doThrow(new AccessDeniedException("not a member")).when(access).requireMember(3L, 1L);
        assertThrows(AccessDeniedException.class, () -> policy.authorize("/topic/juego/3", 1L));
        var send = StompHeaderAccessor.create(StompCommand.SEND); send.setSessionId("session"); send.setDestination("/topic/lobby/2");
        assertThrows(AccessDeniedException.class, () -> policy.preSend(MessageBuilder.createMessage(new byte[0], send.getMessageHeaders()), null));
        var out = StompHeaderAccessor.create(StompCommand.MESSAGE); out.setSessionId("session"); out.setDestination("/topic/lobby/2");
        var message = MessageBuilder.createMessage(new byte[0], out.getMessageHeaders());
        assertNotNull(policy.outbound().preSend(message, null));
        user.setTokenVersion(2);
        assertNull(policy.outbound().preSend(message, null));
        user.setTokenVersion(1);
        doThrow(new AccessDeniedException("expelled")).when(access).requireMember(2L, 1L);
        assertNull(policy.outbound().preSend(message, null));
    }
}
