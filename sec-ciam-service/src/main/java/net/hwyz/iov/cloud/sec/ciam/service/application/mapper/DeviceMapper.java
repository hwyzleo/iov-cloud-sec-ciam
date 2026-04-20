package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.api.vo.DeviceVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceMapper {
    
    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);
    
    Device toDomain(CiamDeviceDo entity);
    
    CiamDeviceDo toDo(Device domain);
    
    DeviceVO toVo(Device domain);
}
