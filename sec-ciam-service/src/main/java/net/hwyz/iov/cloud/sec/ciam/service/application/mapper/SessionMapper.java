package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.SessionVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.SessionDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamSessionDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 会话 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {
    
    SessionMapper INSTANCE = Mappers.getMapper(SessionMapper.class);
    
    /**
     * DO -> Domain
     */
    Session toDomain(CiamSessionDo entity);
    
    /**
     * Domain -> DO
     */
    CiamSessionDo toDo(Session domain);

    /**
     * Domain -> DTO
     */
    SessionDto toDto(Session domain);

    /**
     * DTO -> Domain
     */
    Session toEntity(SessionDto dto);

    /**
     * DTO -> VO
     */
    SessionVo toVo(SessionDto dto);
}
