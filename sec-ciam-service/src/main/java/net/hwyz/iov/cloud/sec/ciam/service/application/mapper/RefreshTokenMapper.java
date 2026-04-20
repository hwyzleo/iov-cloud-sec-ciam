package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.api.vo.RefreshTokenVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenMapper {
    
    RefreshTokenMapper INSTANCE = Mappers.getMapper(RefreshTokenMapper.class);
    
    RefreshToken toDomain(CiamRefreshTokenDo entity);
    
    CiamRefreshTokenDo toDo(RefreshToken domain);
    
    RefreshTokenVO toVo(RefreshToken domain);
}
