package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserPo;
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
    User toDomain(UserPo entity);
    
    /**
     * Domain -> DO
     */
    UserPo toDo(User domain);

    /**
     * Domain -> DTO
     */
    UserDto toDto(User domain);

    /**
     * DTO -> Domain
     */
    User toEntity(UserDto dto);

    /**
     * DTO -> VO
     */
    UserVo toVo(UserDto dto);
}
