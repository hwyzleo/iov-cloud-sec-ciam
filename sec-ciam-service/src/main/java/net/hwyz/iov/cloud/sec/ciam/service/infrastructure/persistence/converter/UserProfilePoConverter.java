package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserProfilePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfilePoConverter {
    UserProfilePoConverter INSTANCE = Mappers.getMapper(UserProfilePoConverter.class);
    UserProfile toDomain(UserProfilePo po);
    UserProfilePo toPo(UserProfile domain);
}
