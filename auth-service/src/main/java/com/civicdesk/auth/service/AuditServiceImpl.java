package com.civicdesk.auth.service;

import com.civicdesk.auth.dto.response.AuditLogResponse;
import com.civicdesk.auth.entity.AuditLog;
import com.civicdesk.auth.repository.AuditLogRepository;
import com.civicdesk.auth.repository.spec.AuditLogSpecifications;
import com.civicdesk.auth.response.PageResponse;
import com.civicdesk.auth.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String userId, String action, String module, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }

    @Override
    public PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size) {
        Specification<AuditLog> spec = Specification.where(null);
        if (userId != null && !userId.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasUserId(userId.trim()));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasAction(action));
        }
        if (module != null && !module.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasModule(module));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(logs, AuditLogResponse::from);
    }

    @Override
    public AuditLogResponse getById(String id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        return AuditLogResponse.from(log);
    }
}
