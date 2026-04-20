package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamSessionDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {
    
    SessionMapper INSTANCE = Mappers.getMapper(SessionMapper.class);
    
    Session toDomain(CiamSessionDo entity);
    
    CiamSessionDo toDo(Session domain);
    
    SessionVO toVo(Session domain);
}
