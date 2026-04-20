package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.api.vo.UserProfileVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    
    UserProfileMapper INSTANCE = Mappers.getMapper(UserProfileMapper.class);
    
    UserProfile toDomain(CiamUserProfileDo entity);
    
    CiamUserProfileDo toDo(UserProfile domain);
    
    UserProfileVO toVo(UserProfile domain);
}
