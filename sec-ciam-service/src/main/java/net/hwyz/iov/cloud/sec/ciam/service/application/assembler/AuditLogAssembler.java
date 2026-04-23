package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.AuditLogVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AuditLogDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuditLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 审计日志 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogAssembler {
    
    AuditLogAssembler INSTANCE = Mappers.getMapper(AuditLogAssembler.class);
    
    /**
     * DO -> Domain
     */
    AuditLog toDomain(AuditLogPo entity);
    
    /**
     * Domain -> DO
     */
    AuditLogPo toDo(AuditLog domain);

    /**
     * Domain -> DTO
     */
    AuditLogDto2 toDto(AuditLog domain);

    /**
     * DTO -> Domain
     */
    AuditLog toEntity(AuditLogDto2 dto);

    /**
     * DTO -> VO
     */
    AuditLogVo toVo(AuditLogDto2 dto);
}
