package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskEventPoConverter {
    RiskEventPoConverter INSTANCE = Mappers.getMapper(RiskEventPoConverter.class);
    RiskEvent toDomain(RiskEventPo po);
    RiskEventPo toPo(RiskEvent domain);
}
