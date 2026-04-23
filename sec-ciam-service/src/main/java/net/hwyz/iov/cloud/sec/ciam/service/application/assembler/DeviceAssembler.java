package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.DeviceVo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DevicePo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 设备 Mapper
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceAssembler {
    
    DeviceAssembler INSTANCE = Mappers.getMapper(DeviceAssembler.class);
    
    /**
     * DO -> Domain
     */
    Device toDomain(DevicePo entity);
    
    /**
     * Domain -> DO
     */
    DevicePo toDo(Device domain);

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
