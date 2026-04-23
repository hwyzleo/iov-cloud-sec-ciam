package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OAuthClientPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OAuthClientPoConverter {
    OAuthClientPoConverter INSTANCE = Mappers.getMapper(OAuthClientPoConverter.class);
    OAuthClient toDomain(OAuthClientPo po);
    OAuthClientPo toPo(OAuthClient domain);
}
