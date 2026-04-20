package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConsentMapper {
    
    UserConsentMapper INSTANCE = Mappers.getMapper(UserConsentMapper.class);
    
    UserConsent toDomain(CiamUserConsentDo entity);
    
    CiamUserConsentDo toDo(UserConsent domain);
}
