package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserTag;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserTagPoConverter {
    UserTagPoConverter INSTANCE = Mappers.getMapper(UserTagPoConverter.class);
    UserTag toDomain(UserTagPo po);
    UserTagPo toPo(UserTag domain);
}
