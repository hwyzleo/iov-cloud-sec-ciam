package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserIdentityVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserIdentityDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户身份 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserIdentityMapper {
    
    UserIdentityMapper INSTANCE = Mappers.getMapper(UserIdentityMapper.class);
    
    /**
     * DO -> Domain
     */
    UserIdentity toDomain(CiamUserIdentityDo entity);
    
    /**
     * Domain -> DO
     */
    CiamUserIdentityDo toDo(UserIdentity domain);

    /**
     * Domain -> DTO
     */
    UserIdentityDto toDto(UserIdentity domain);

    /**
     * DTO -> Domain
     */
    UserIdentity toEntity(UserIdentityDto dto);

    /**
     * DTO -> VO
     */
    UserIdentityVo toVo(UserIdentityDto dto);
}
