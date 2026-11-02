package com.sstc.platform.common.web;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.domain.AuditLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-cutting audit endpoint. The S02 Operation Logger and the S01 access audit have their
 * own endpoints; this one exposes the unified platform trail.
 */
@RestController
@RequestMapping("/api/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "60") int limit,
                                          @RequestParam(required = false) String module,
                                          @RequestParam(required = false) String action,
                                          @RequestParam(required = false) Long actorId) {
        List<AuditLog> logs = module == null && action == null && actorId == null
                ? auditService.recent(limit)
                : auditService.search(module, action, actorId, limit);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (AuditLog log : logs) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("id", log.getId());
            map.put("module", log.getModule());
            map.put("action", log.getAction());
            map.put("actorId", log.getActorId());
            map.put("actorName", log.getActorName());
            map.put("targetType", log.getTargetType());
            map.put("targetId", log.getTargetId());
            map.put("detail", log.getDetail());
            map.put("outcome", log.getOutcome());
            map.put("createdAt", log.getCreatedAt());
            result.add(map);
        }
        return result;
    }
}
