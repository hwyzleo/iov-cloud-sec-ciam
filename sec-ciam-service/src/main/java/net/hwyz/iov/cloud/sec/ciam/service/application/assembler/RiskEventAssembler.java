package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.RiskEventVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RiskEventDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 风险事件 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskEventAssembler {
    
    RiskEventAssembler INSTANCE = Mappers.getMapper(RiskEventAssembler.class);
    
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
    RiskEventDto2 toDto(RiskEvent domain);

    /**
     * DTO -> Domain
     */
    RiskEvent toEntity(RiskEventDto2 dto);

    /**
     * DTO -> VO
     */
    RiskEventVo toVo(RiskEventDto2 dto);
}
