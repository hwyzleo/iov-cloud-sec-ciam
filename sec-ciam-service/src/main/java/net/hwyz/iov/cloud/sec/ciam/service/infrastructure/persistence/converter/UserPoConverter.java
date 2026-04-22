package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserPoConverter {
    UserPoConverter INSTANCE = Mappers.getMapper(UserPoConverter.class);
    User toDomain(UserPo po);
    UserPo toPo(User domain);
}
