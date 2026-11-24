package com.synthetic.readiness.readiness;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

@Service
public class ActionConcurrencyService {
    public record Patch(String owner, LocalDate dueDate, String status, String evidenceReference,
                        String completedBy, Boolean exceptionAccepted, String exceptionApprover,
                        String exceptionRationale) {}
    public record UpdateResult(ReadinessAction action, int retries, long elapsedNanos) {}

    private static final int MAX_ATTEMPTS = 5;
    private final TransactionTemplate transactions;
    private final WorkbenchService workbench;
    private final LongAdder optimisticConflicts = new LongAdder();
    private final LongAdder optimisticRetries = new LongAdder();
    @PersistenceContext private EntityManager em;

    public ActionConcurrencyService(PlatformTransactionManager transactionManager, WorkbenchService workbench) {
        this.transactions = new TransactionTemplate(transactionManager);
        this.workbench = workbench;
    }

    public UpdateResult update(long id, Patch patch, String actor, String role) {
        return update(id, patch, actor, role, LockModeType.NONE);
    }

    /** Benchmark/comparison path for the same row update, using PostgreSQL SELECT FOR UPDATE. */
    public UpdateResult updatePessimistic(long id, Patch patch, String actor, String role) {
        return update(id, patch, actor, role, LockModeType.PESSIMISTIC_WRITE);
    }

    public long optimisticConflicts() { return optimisticConflicts.sum(); }
    public long optimisticRetries() { return optimisticRetries.sum(); }

    private UpdateResult update(long id, Patch patch, String actor, String role, LockModeType lockMode) {
        long started = System.nanoTime();
        int retries = 0;
        for (int attempt = 1; ; attempt++) {
            try {
                ReadinessAction action = transactions.execute(status -> {
                    ReadinessAction row = lockMode == LockModeType.NONE
                        ? em.find(ReadinessAction.class, id)
                        : em.find(ReadinessAction.class, id, lockMode);
                    if (row == null) throw new java.util.NoSuchElementException("Readiness action not found");
                    String before = summary(row);
                    apply(row, patch);
                    row.updateCount++;
                    em.flush();
                    workbench.audit(actor, role, "ReadinessAction", id, "UPDATE", before, summary(row));
                    return row;
                });
                return new UpdateResult(action, retries, System.nanoTime() - started);
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException conflict) {
                optimisticConflicts.increment();
                if (attempt >= MAX_ATTEMPTS) throw conflict;
                optimisticRetries.increment();
                retries++;
                LockSupport.parkNanos(ThreadLocalRandom.current().nextLong(1_000_000, 10_000_001));
            }
        }
    }

    private static void apply(ReadinessAction action, Patch patch) {
        if (patch.owner() != null) action.owner = patch.owner();
        if (patch.dueDate() != null) action.dueDate = patch.dueDate();
        if (patch.evidenceReference() != null) action.evidenceReference = patch.evidenceReference();
        if (patch.completedBy() != null) action.completedBy = patch.completedBy();
        if (patch.exceptionAccepted() != null) action.exceptionAccepted = patch.exceptionAccepted();
        if (patch.exceptionApprover() != null) action.exceptionApprover = patch.exceptionApprover();
        if (patch.exceptionRationale() != null) action.exceptionRationale = patch.exceptionRationale();
        if ("COMPLETE".equalsIgnoreCase(patch.status())) {
            boolean evidence = action.completedBy != null && !action.completedBy.isBlank()
                && action.evidenceReference != null && !action.evidenceReference.isBlank();
            boolean exception = action.exceptionAccepted && action.exceptionApprover != null
                && !action.exceptionApprover.isBlank() && action.exceptionRationale != null
                && !action.exceptionRationale.isBlank();
            if (!evidence && !exception)
                throw new IllegalArgumentException("Completion requires owner completion and evidence, or an accepted exception with approver and rationale.");
        }
        if (patch.status() != null) action.status = patch.status().toUpperCase(Locale.ROOT);
        if ("COMPLETE".equals(action.status)) action.completedAt = LocalDateTime.now();
    }

    private static String summary(ReadinessAction action) {
        return "status=" + action.status + ", owner=" + action.owner + ", evidence=" + action.evidenceReference
            + ", exception=" + action.exceptionAccepted;
    }
}
