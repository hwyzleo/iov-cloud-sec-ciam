package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamAuditLogDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {
    
    AuditLogMapper INSTANCE = Mappers.getMapper(AuditLogMapper.class);
    
    AuditLog toDomain(CiamAuditLogDo entity);
    
    CiamAuditLogDo toDo(AuditLog domain);
}
