package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertState;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OwnerCertStatePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OwnerCertStatePoConverter {
    OwnerCertStatePoConverter INSTANCE = Mappers.getMapper(OwnerCertStatePoConverter.class);
    OwnerCertState toDomain(OwnerCertStatePo po);
    OwnerCertStatePo toPo(OwnerCertState domain);
}
