package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Jwk;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JwkPoConverter {
    JwkPoConverter INSTANCE = Mappers.getMapper(JwkPoConverter.class);
    Jwk toDomain(JwkPo po);
    JwkPo toPo(Jwk domain);
}
