package net.hwyz.iov.cloud.sec.ciam.service.application.mapper;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeviceVo;
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
    DeviceInfoDto toDto(Device domain);

    /**
     * DTO -> Domain
     */
    Device toEntity(DeviceInfoDto dto);

    /**
     * DTO -> VO
     */
    DeviceVo toVo(DeviceInfoDto dto);
}
