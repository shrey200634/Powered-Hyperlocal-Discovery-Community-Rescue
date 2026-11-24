package com.communitynexus.common.tx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(String what, Runnable action) {
        Runnable safe = safe(what, action);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }

    public static void onRollback(String what, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        Runnable safe = safe(what, action);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    safe.run();
                }
            }
        });
    }

    private static Runnable safe(String what, Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (Exception e) {
                log.warn("Transaction side-effect '{}' failed: {}", what, e.getMessage());
            }
        };
    }
}