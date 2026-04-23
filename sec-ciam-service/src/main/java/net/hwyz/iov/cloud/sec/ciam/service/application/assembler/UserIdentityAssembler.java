package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserIdentityVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserIdentityDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户身份 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserIdentityAssembler {
    
    UserIdentityAssembler INSTANCE = Mappers.getMapper(UserIdentityAssembler.class);
    
    /**
     * DO -> Domain
     */
    UserIdentity toDomain(UserIdentityPo entity);
    
    /**
     * Domain -> DO
     */
    UserIdentityPo toDo(UserIdentity domain);

    /**
     * Domain -> DTO
     */
    UserIdentityDto2 toDto(UserIdentity domain);

    /**
     * DTO -> Domain
     */
    UserIdentity toEntity(UserIdentityDto2 dto);

    /**
     * DTO -> VO
     */
    UserIdentityVo toVo(UserIdentityDto2 dto);
}
