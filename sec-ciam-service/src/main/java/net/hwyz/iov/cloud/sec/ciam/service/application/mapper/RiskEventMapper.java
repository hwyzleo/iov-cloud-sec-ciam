package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.api.vo.RiskEventVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskEventMapper {
    
    RiskEventMapper INSTANCE = Mappers.getMapper(RiskEventMapper.class);
    
    RiskEvent toDomain(CiamRiskEventDo entity);
    
    CiamRiskEventDo toDo(RiskEvent domain);
    
    RiskEventVO toVo(RiskEvent domain);
}
