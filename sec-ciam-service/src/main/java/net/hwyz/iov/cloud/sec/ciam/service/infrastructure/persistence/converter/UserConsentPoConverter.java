package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConsentPoConverter {
    UserConsentPoConverter INSTANCE = Mappers.getMapper(UserConsentPoConverter.class);
    UserConsent toDomain(UserConsentPo po);
    UserConsentPo toPo(UserConsent domain);
}
