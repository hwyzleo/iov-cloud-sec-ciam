package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserProfileVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserProfileDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserProfilePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户详情 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    
    UserProfileMapper INSTANCE = Mappers.getMapper(UserProfileMapper.class);
    
    /**
     * DO -> Domain
     */
    UserProfile toDomain(UserProfilePo entity);
    
    /**
     * Domain -> DO
     */
    UserProfilePo toDo(UserProfile domain);

    /**
     * Domain -> DTO
     */
    UserProfileDto toDto(UserProfile domain);

    /**
     * DTO -> Domain
     */
    UserProfile toEntity(UserProfileDto dto);

    /**
     * DTO -> VO
     */
    UserProfileVo toVo(UserProfileDto dto);
}
