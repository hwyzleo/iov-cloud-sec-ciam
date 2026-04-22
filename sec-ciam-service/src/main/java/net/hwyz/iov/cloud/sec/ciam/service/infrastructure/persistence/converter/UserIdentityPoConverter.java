package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserIdentityPoConverter {
    UserIdentityPoConverter INSTANCE = Mappers.getMapper(UserIdentityPoConverter.class);
    UserIdentity toDomain(UserIdentityPo po);
    UserIdentityPo toPo(UserIdentity domain);
}
