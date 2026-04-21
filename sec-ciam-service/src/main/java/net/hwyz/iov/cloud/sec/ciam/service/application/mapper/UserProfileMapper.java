package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserProfileVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserProfileDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
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
    UserProfile toDomain(CiamUserProfileDo entity);
    
    /**
     * Domain -> DO
     */
    CiamUserProfileDo toDo(UserProfile domain);

    /**
     * Domain -> DTO
     */
    UserProfileDTO toDto(UserProfile domain);

    /**
     * DTO -> Domain
     */
    UserProfile toEntity(UserProfileDTO dto);

    /**
     * DTO -> VO
     */
    UserProfileVO toVo(UserProfileDTO dto);
}
