package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.DevicePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备表 Mapper。
 */
@Mapper
public interface CiamDeviceMapper extends BaseMapper<DevicePo> {
}
