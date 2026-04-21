package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.RefreshTokenVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RefreshTokenDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 刷新令牌 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenMapper {
    
    RefreshTokenMapper INSTANCE = Mappers.getMapper(RefreshTokenMapper.class);
    
    /**
     * DO -> Domain
     */
    RefreshToken toDomain(CiamRefreshTokenDo entity);
    
    /**
     * Domain -> DO
     */
    CiamRefreshTokenDo toDo(RefreshToken domain);

    /**
     * Domain -> DTO
     */
    RefreshTokenDTO toDto(RefreshToken domain);

    /**
     * DTO -> Domain
     */
    RefreshToken toEntity(RefreshTokenDTO dto);

    /**
     * DTO -> VO
     */
    RefreshTokenVO toVo(RefreshTokenDTO dto);
}
