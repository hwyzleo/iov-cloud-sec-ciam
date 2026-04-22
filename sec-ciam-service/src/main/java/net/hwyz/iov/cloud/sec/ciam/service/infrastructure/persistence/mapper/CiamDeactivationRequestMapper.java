package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DeactivationRequestPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 注销申请表 Mapper。
 */
@Mapper
public interface CiamDeactivationRequestMapper extends BaseMapper<DeactivationRequestPo> {
}
