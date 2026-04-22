package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserCredential;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCredentialPoConverter {
    UserCredentialPoConverter INSTANCE = Mappers.getMapper(UserCredentialPoConverter.class);
    UserCredential toDomain(UserCredentialPo po);
    UserCredentialPo toPo(UserCredential domain);
}
