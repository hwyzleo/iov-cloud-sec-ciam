package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 用户 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    /**
     * DO -> Domain
     */
    User toDomain(CiamUserDo entity);
    
    /**
     * Domain -> DO
     */
    CiamUserDo toDo(User domain);

    /**
     * Domain -> DTO
     */
    UserDTO toDto(User domain);

    /**
     * DTO -> Domain
     */
    User toEntity(UserDTO dto);

    /**
     * DTO -> VO
     */
    UserVO toVo(UserDTO dto);
}
