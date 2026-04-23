package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.RefreshTokenVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RefreshTokenDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 刷新令牌 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenAssembler {
    
    RefreshTokenAssembler INSTANCE = Mappers.getMapper(RefreshTokenAssembler.class);
    
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
    RefreshTokenDto2 toDto(RefreshToken domain);

    /**
     * DTO -> Domain
     */
    RefreshToken toEntity(RefreshTokenDto2 dto);

    /**
     * DTO -> VO
     */
    RefreshTokenVo toVo(RefreshTokenDto2 dto);
}
