package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenPoConverter {
    RefreshTokenPoConverter INSTANCE = Mappers.getMapper(RefreshTokenPoConverter.class);
    RefreshToken toDomain(RefreshTokenPo po);
    RefreshTokenPo toPo(RefreshToken domain);
}
