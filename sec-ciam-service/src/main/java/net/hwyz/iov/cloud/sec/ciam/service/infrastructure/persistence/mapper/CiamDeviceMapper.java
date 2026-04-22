package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DevicePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备表 Mapper。
 */
@Mapper
public interface CiamDeviceMapper extends BaseMapper<DevicePo> {
}
