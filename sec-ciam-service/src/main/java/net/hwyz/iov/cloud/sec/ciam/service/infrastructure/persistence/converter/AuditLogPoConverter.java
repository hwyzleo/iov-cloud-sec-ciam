package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuditLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogPoConverter {
    AuditLogPoConverter INSTANCE = Mappers.getMapper(AuditLogPoConverter.class);
    AuditLog toDomain(AuditLogPo po);
    AuditLogPo toPo(AuditLog domain);
}
