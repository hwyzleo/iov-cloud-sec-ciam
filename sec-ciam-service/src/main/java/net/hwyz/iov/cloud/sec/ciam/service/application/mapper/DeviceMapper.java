package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDTO;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeviceVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 设备 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceMapper {
    
    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);
    
    /**
     * DO -> Domain
     */
    Device toDomain(CiamDeviceDo entity);
    
    /**
     * Domain -> DO
     */
    CiamDeviceDo toDo(Device domain);

    /**
     * Domain -> DTO
     */
    DeviceInfoDTO toDto(Device domain);

    /**
     * DTO -> Domain
     */
    Device toEntity(DeviceInfoDTO dto);

    /**
     * DTO -> VO
     */
    DeviceVO toVo(DeviceInfoDTO dto);
}
