package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuthCode;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuthCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthCodePoConverter {
    AuthCodePoConverter INSTANCE = Mappers.getMapper(AuthCodePoConverter.class);
    AuthCode toDomain(AuthCodePo po);
    AuthCodePo toPo(AuthCode domain);
}
