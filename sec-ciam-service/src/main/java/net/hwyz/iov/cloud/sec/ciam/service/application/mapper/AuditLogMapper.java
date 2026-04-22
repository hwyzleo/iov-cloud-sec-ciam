package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AuditLogDto;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.AuditLogVo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuditLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 审计日志 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {
    
    AuditLogMapper INSTANCE = Mappers.getMapper(AuditLogMapper.class);
    
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
    AuditLogDto toDto(AuditLog domain);

    /**
     * DTO -> Domain
     */
    AuditLog toEntity(AuditLogDto dto);

    /**
     * DTO -> VO
     */
    AuditLogVo toVo(AuditLogDto dto);
}
