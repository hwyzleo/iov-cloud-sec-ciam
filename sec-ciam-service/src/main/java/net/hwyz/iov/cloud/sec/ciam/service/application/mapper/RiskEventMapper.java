package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.RiskEventVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RiskEventDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.RiskEventPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 风险事件 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskEventMapper {
    
    RiskEventMapper INSTANCE = Mappers.getMapper(RiskEventMapper.class);
    
    /**
     * DO -> Domain
     */
    RiskEvent toDomain(RiskEventPo entity);
    
    /**
     * Domain -> DO
     */
    RiskEventPo toDo(RiskEvent domain);

    /**
     * Domain -> DTO
     */
    RiskEventDto toDto(RiskEvent domain);

    /**
     * DTO -> Domain
     */
    RiskEvent toEntity(RiskEventDto dto);

    /**
     * DTO -> VO
     */
    RiskEventVo toVo(RiskEventDto dto);
}
