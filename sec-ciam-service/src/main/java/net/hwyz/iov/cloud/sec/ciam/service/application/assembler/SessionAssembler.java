package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.SessionVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.SessionDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 会话 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionAssembler {
    
    SessionAssembler INSTANCE = Mappers.getMapper(SessionAssembler.class);
    
    /**
     * DO -> Domain
     */
    Session toDomain(SessionPo entity);
    
    /**
     * Domain -> DO
     */
    SessionPo toDo(Session domain);

    /**
     * Domain -> DTO
     */
    SessionDto2 toDto(Session domain);

    /**
     * DTO -> Domain
     */
    Session toEntity(SessionDto2 dto);

    /**
     * DTO -> VO
     */
    SessionVo toVo(SessionDto2 dto);
}
