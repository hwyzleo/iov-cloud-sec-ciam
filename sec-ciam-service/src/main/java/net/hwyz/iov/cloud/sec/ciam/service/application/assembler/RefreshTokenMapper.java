package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.RefreshTokenVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RefreshTokenDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
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
    RefreshToken toDomain(RefreshTokenPo entity);
    
    /**
     * Domain -> DO
     */
    RefreshTokenPo toDo(RefreshToken domain);

    /**
     * Domain -> DTO
     */
    RefreshTokenDto toDto(RefreshToken domain);

    /**
     * DTO -> Domain
     */
    RefreshToken toEntity(RefreshTokenDto dto);

    /**
     * DTO -> VO
     */
    RefreshTokenVo toVo(RefreshTokenDto dto);
}
