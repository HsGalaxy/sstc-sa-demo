package com.sstc.platform.common.audit;

import com.sstc.platform.common.domain.AuditLog;
import com.sstc.platform.common.repo.AuditLogRepository;
import com.sstc.platform.common.security.CurrentUser;
import com.sstc.platform.common.security.CurrentUserHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cross-cutting audit writer.
 *
 * <p>It backs the {@code TaskDashboard.OperationLogger} (S02 US04), the
 * {@code UserRoleManager.AuthorizationAuditController} (S01 US04) and the Toolbox
 * integration log. Audit writes run in their own transaction so a failed business
 * operation still leaves a trace.</p>
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String module, String action, String targetType, String targetId,
                       String detail, String outcome) {
        CurrentUser actor = CurrentUserHolder.get();
        AuditLog log = new AuditLog();
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setOutcome(outcome == null ? "SUCCESS" : outcome);
        if (actor != null) {
            log.setActorId(actor.getId());
            log.setActorName(actor.getDisplayName());
        } else {
            log.setActorName("system");
        }
        repository.save(log);
    }

    public void success(String module, String action, String targetType, String targetId, String detail) {
        record(module, action, targetType, targetId, detail, "SUCCESS");
    }

    public void failure(String module, String action, String targetType, String targetId, String detail) {
        record(module, action, targetType, targetId, detail, "FAILURE");
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String module, String action, Long actorId, int limit) {
        return repository.search(module, action, actorId, PageRequest.of(0, limit));
    }
}
