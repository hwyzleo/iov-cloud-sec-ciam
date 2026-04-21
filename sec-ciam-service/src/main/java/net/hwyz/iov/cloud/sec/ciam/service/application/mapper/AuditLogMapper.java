package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.AuditLogVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AuditLogDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamAuditLogDo;
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
    AuditLog toDomain(CiamAuditLogDo entity);
    
    /**
     * Domain -> DO
     */
    CiamAuditLogDo toDo(AuditLog domain);

    /**
     * Domain -> DTO
     */
    AuditLogDTO toDto(AuditLog domain);

    /**
     * DTO -> Domain
     */
    AuditLog toEntity(AuditLogDTO dto);

    /**
     * DTO -> VO
     */
    AuditLogVO toVo(AuditLogDTO dto);
}
