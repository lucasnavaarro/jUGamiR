package com.jugamir.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Never publish state which can still be rolled back by the database. */
@Component
@RequiredArgsConstructor
public class CommittedMessages {
    private final SimpMessagingTemplate template;

    public void send(String destination, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            template.convertAndSend(destination, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { template.convertAndSend(destination, payload); }
        });
    }
}
